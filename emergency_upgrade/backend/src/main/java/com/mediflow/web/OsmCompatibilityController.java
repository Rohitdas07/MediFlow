package com.mediflow.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.model.Hospital;
import com.mediflow.repository.HospitalRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api")
public class OsmCompatibilityController {
    private final HospitalRepository hospitalRepository;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public OsmCompatibilityController(HospitalRepository hospitalRepository, ObjectMapper mapper) {
        this.hospitalRepository = hospitalRepository; this.mapper = mapper;
    }

    @PostMapping("/geocode/reverse")
    public Map<String,Object> reverse(@RequestBody Map<String,Object> body) {
        double lat = number(body.get("lat")); double lng = number(body.get("lng"));
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + lat + "&lon=" + lng;
            JsonNode n = getJson(url);
            Map<String,Object> a = new LinkedHashMap<>();
            JsonNode addr = n.path("address");
            a.put("formattedAddress", n.path("display_name").asText("Current location"));
            a.put("displayName", n.path("display_name").asText("Current GPS Location"));
            a.put("area", first(addr,"suburb","neighbourhood","village","town"));
            a.put("city", first(addr,"city","town","municipality","county"));
            a.put("state", addr.path("state").asText(""));
            a.put("country", addr.path("country").asText("India"));
            a.put("latitude", lat); a.put("longitude", lng); a.put("isManual", false);
            return Map.of("success", true, "location", a);
        } catch (Exception e) {
            Map<String,Object> a = new LinkedHashMap<>();
            a.put("formattedAddress", String.format(Locale.US,"Coordinates (%.4f, %.4f)",lat,lng));
            a.put("displayName", "Your Current GPS Location"); a.put("area", "Current Area"); a.put("city", "Detected Location");
            a.put("state", ""); a.put("country", "India"); a.put("latitude",lat); a.put("longitude",lng); a.put("isManual",false);
            return Map.of("success", true, "location", a, "message", "OSM reverse geocoding unavailable; GPS coordinates retained.");
        }
    }

    @PostMapping("/geocode/forward")
    public Map<String,Object> forward(@RequestBody Map<String,Object> body) {
        String query = String.valueOf(body.getOrDefault("query", "")).trim();
        if (query.isBlank()) return Map.of("success", false, "error", "Location query is required.");
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=in&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            JsonNode n = getJson(url);
            if (!n.isArray() || n.isEmpty()) return Map.of("success", false, "error", "No results found for " + query + ".");
            JsonNode x=n.get(0); double lat=x.path("lat").asDouble(); double lon=x.path("lon").asDouble();
            Map<String,Object> loc=new LinkedHashMap<>(); loc.put("formattedAddress",x.path("display_name").asText(query)); loc.put("displayName",x.path("display_name").asText(query));
            loc.put("area", ""); loc.put("city", ""); loc.put("state", ""); loc.put("country", "India"); loc.put("latitude",lat); loc.put("longitude",lon); loc.put("isManual",true);
            return Map.of("success",true,"location",loc);
        } catch(Exception e) { return Map.of("success",false,"error","OpenStreetMap geocoding is temporarily unavailable."); }
    }

    @PostMapping("/hospitals/nearby")
    public Map<String,Object> nearby(@RequestBody Map<String,Object> body) {
        double lat=number(body.get("lat")), lng=number(body.get("lng")); int radius=(int)number(body.getOrDefault("radius",5000));
        boolean hospitalsOnly=Boolean.TRUE.equals(body.get("hospitalsOnly"));
        try {
            String q="[out:json][timeout:12];(nwr[amenity=hospital](around:"+radius+","+lat+","+lng+");nwr[healthcare=hospital](around:"+radius+","+lat+","+lng+");"+(hospitalsOnly?"":"nwr[amenity=clinic](around:"+radius+","+lat+","+lng+");nwr[healthcare=centre](around:"+radius+","+lat+","+lng+");")+");out center tags;";
            JsonNode root = null;
            Exception lastOverpassError = null;
            String[] overpassEndpoints = {
                "https://overpass-api.de/api/interpreter",
                "https://overpass.kumi.systems/api/interpreter",
                "https://overpass.private.coffee/api/interpreter"
            };
            for (String endpoint : overpassEndpoints) {
                try {
                    root = postJson(endpoint, q, "application/x-www-form-urlencoded");
                    if (root != null && root.has("elements")) break;
                } catch (Exception ex) {
                    lastOverpassError = ex;
                }
            }
            List<Map<String,Object>> hospitals=new ArrayList<>();
            if (root == null || !root.has("elements")) {
                // Overpass can be rate-limited. Fall back to real Nominatim hospital
                // records instead of returning seeded/demo hospitals at the wrong place.
                double latDelta = radius / 111000.0;
                double lngDelta = radius / (111000.0 * Math.max(0.2, Math.cos(Math.toRadians(lat))));
                String viewbox = String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", lng-lngDelta, lat+latDelta, lng+lngDelta, lat-latDelta);
                String searchUrl="https://nominatim.openstreetmap.org/search?format=jsonv2&limit=50&countrycodes=in&bounded=1&viewbox="+viewbox+"&q=hospital";
                JsonNode nominatim=getJson(searchUrl);
                if (nominatim.isArray()) {
                    for (JsonNode x : nominatim) {
                        double hpLat=x.path("lat").asDouble(Double.NaN), hpLng=x.path("lon").asDouble(Double.NaN);
                        if (!Double.isFinite(hpLat) || !Double.isFinite(hpLng)) continue;
                        double d=distance(lat,lng,hpLat,hpLng);
                        if (d*1000 > radius) continue;
                        String name=x.path("name").asText("").trim();
                        if (name.isBlank()) continue;
                        Map<String,Object> h=new LinkedHashMap<>();
                        h.put("id","osm-"+x.path("osm_id").asText());
                        h.put("name",name);
                        h.put("type","hospital"); h.put("typeLabel","Hospital");
                        h.put("isHospital",true); h.put("isSpecialtyClinic",false); h.put("isEmergencyVerified",false);
                        h.put("address",x.path("display_name").asText(name));
                        h.put("latitude",hpLat); h.put("longitude",hpLng);
                        h.put("source","openstreetmap");
                        h.put("emergencyAvailable","Emergency capability: Not verified");
                        h.put("googleMapsURI","https://www.openstreetmap.org/?mlat="+hpLat+"&mlon="+hpLng);
                        h.put("distanceKm",d); hospitals.add(h);
                    }
                }
                hospitals.sort(Comparator.comparingDouble(h->((Number)h.get("distanceKm")).doubleValue()));
                return Map.of("success",true,"count",hospitals.size(),"provider","openstreetmap","searchRadiusMeters",radius,
                    "message","Nearby hospital results sourced from OpenStreetMap Nominatim fallback.","hospitals",hospitals);
            }
            for(JsonNode x: root.path("elements")) {
                double[] p=coords(x); if(p==null) continue;
                JsonNode t=x.path("tags"); String name=t.path("name").asText("").trim(); if(name.isBlank()) continue;
                String amenity=t.path("amenity").asText("").toLowerCase(Locale.ROOT);
                String healthcare=t.path("healthcare").asText("").toLowerCase(Locale.ROOT);
                String lowerName=name.toLowerCase(Locale.ROOT);
                boolean obviousNonHospital = lowerName.matches(".*\\b(pharmacy|chemist|medical store|diagnostic|pathology|laboratory|lab|dental|eye clinic|optical|physiotherapy)\\b.*");
                boolean trueHospital = "hospital".equals(amenity) || "hospital".equals(healthcare) || lowerName.matches(".*\\b(hospital|medical college|aiims|trauma centre|trauma center|nursing home|health institute)\\b.*");
                if (hospitalsOnly && (!trueHospital || obviousNonHospital)) continue;
                Map<String,Object> h=new LinkedHashMap<>(); h.put("id","osm-"+x.path("id").asText()); h.put("name",name);
                h.put("type", "clinic".equals(amenity)?"clinic":("centre".equals(healthcare)?"health_centre":"hospital"));
                h.put("typeLabel", trueHospital ? "Hospital" : ("centre".equals(healthcare) ? "Health Centre" : "Clinic"));
                h.put("isHospital", trueHospital); h.put("isSpecialtyClinic", !trueHospital); h.put("isEmergencyVerified", false);
                h.put("address", address(t)); h.put("latitude",p[0]); h.put("longitude",p[1]); h.put("phone",t.path("phone").asText(t.path("contact:phone").asText("")));
                h.put("source","openstreetmap"); h.put("emergencyAvailable","OpenStreetMap facility listing"); h.put("googleMapsURI","https://www.openstreetmap.org/?mlat="+p[0]+"&mlon="+p[1]);
                h.put("distanceKm", distance(lat,lng,p[0],p[1])); hospitals.add(h);
            }
            hospitals.sort(Comparator.comparingDouble(h->((Number)h.get("distanceKm")).doubleValue()));
            return Map.of("success",true,"count",hospitals.size(),"provider","openstreetmap","searchRadiusMeters",radius,"hospitals",hospitals);
        } catch(Exception e) {
            return Map.of("success",false,"count",0,"provider","none","searchRadiusMeters",radius,
                "errorCode","HOSPITAL_DISCOVERY_UNAVAILABLE",
                "message","Live hospital discovery is temporarily unavailable. No demo/seed hospitals were substituted.");
        }
    }

    @PostMapping("/hospitals/search")
    public Map<String,Object> search(@RequestBody Map<String,Object> body) {
        String query=String.valueOf(body.getOrDefault("query","")).trim(); if(query.isBlank()) return Map.of("success",false,"error","Search query is required.");
        try {
            String url="https://nominatim.openstreetmap.org/search?format=jsonv2&limit=15&countrycodes=in&q="+URLEncoder.encode(query+" hospital",StandardCharsets.UTF_8);
            JsonNode root=getJson(url); List<Map<String,Object>> list=new ArrayList<>();
            for(JsonNode x:root) { double lat=x.path("lat").asDouble(), lon=x.path("lon").asDouble(); Map<String,Object> h=new LinkedHashMap<>(); h.put("id","osm-"+x.path("osm_id").asText()); h.put("name",x.path("name").asText(query)); h.put("type","hospital"); h.put("typeLabel","Hospital"); h.put("isHospital",true); h.put("address",x.path("display_name").asText()); h.put("latitude",lat); h.put("longitude",lon); h.put("googleMapsURI","https://www.openstreetmap.org/?mlat="+lat+"&mlon="+lon); h.put("source","openstreetmap"); list.add(h); }
            return Map.of("success",true,"provider","openstreetmap","hospitals",list);
        } catch(Exception e) { return Map.of("success",false,"error","OpenStreetMap hospital search is temporarily unavailable."); }
    }

    @PostMapping("/routes/compute")
    public Map<String,Object> route(@RequestBody Map<String,Object> body) {
        try {
            Map<String,Object> o=(Map<String,Object>)body.get("origin"), d=(Map<String,Object>)body.get("destination");
            if (o == null || d == null) return Map.of("success",false,"error","Origin and destination are required.");
            double olat=number(o.get("lat")), olon=number(o.get("lng")), dlat=number(d.get("lat")), dlon=number(d.get("lng"));
            if (!Double.isFinite(olat) || !Double.isFinite(olon) || !Double.isFinite(dlat) || !Double.isFinite(dlon)) {
                return Map.of("success",false,"error","Invalid route coordinates.");
            }
            String mode=String.valueOf(body.getOrDefault("travelMode","ambulance"));
            String[] urls = {
                "https://router.project-osrm.org/route/v1/driving/"+olon+","+olat+";"+dlon+","+dlat+"?overview=full&geometries=geojson",
                "https://routing.openstreetmap.de/routed-car/route/v1/driving/"+olon+","+olat+";"+dlon+","+dlat+"?overview=full&geometries=geojson"
            };
            Exception last=null;
            for (String url : urls) {
                try {
                    JsonNode root=getJson(url), r=root.path("routes").isArray() && root.path("routes").size()>0 ? root.path("routes").get(0) : null;
                    if (r == null) continue;
                    List<Map<String,Double>> coords=new ArrayList<>();
                    for(JsonNode c:r.path("geometry").path("coordinates")) {
                        if (c.isArray() && c.size() >= 2) coords.add(Map.of("lat", c.get(1).asDouble(), "lng", c.get(0).asDouble()));
                    }
                    if (coords.size() < 2) continue;
                    double meters=r.path("distance").asDouble(), seconds=r.path("duration").asDouble();
                    if("ambulance".equals(mode)) seconds=Math.max(60,seconds*.75);
                    int mins=Math.max(1,(int)Math.round(seconds/60));
                    Map<String,Object> out=new LinkedHashMap<>();
                    out.put("success",true); out.put("distanceMeters",Math.round(meters));
                    out.put("distanceKm",Math.round(meters/10.0)/100.0);
                    out.put("distanceText",meters<1000?Math.round(meters)+" m":String.format(Locale.US,"%.1f km",meters/1000));
                    out.put("durationSeconds",Math.round(seconds)); out.put("durationMinutes",mins); out.put("durationText",mins+" mins");
                    out.put("points",coords); out.put("travelMode",mode); out.put("source","osrm_backend");
                    return out;
                } catch (Exception ex) { last=ex; }
            }
            return Map.of("success",false,"error","Live road routing is temporarily unavailable.","detail",last == null ? "" : String.valueOf(last.getMessage()));
        } catch(Exception e) {
            return Map.of("success",false,"error","Invalid route request.");
        }
    }

    private JsonNode getJson(String url) throws Exception { HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("User-Agent","MediFlow/1.0 medical application map service").GET().build(); HttpResponse<String> r=client.send(req,HttpResponse.BodyHandlers.ofString()); if(r.statusCode()/100!=2) throw new IllegalStateException("HTTP "+r.statusCode()); return mapper.readTree(r.body()); }
    private JsonNode postJson(String url,String body,String contentType) throws Exception { HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("User-Agent","MediFlow/1.0 medical application map service").header("Content-Type",contentType).POST(HttpRequest.BodyPublishers.ofString("data="+URLEncoder.encode(body,StandardCharsets.UTF_8))).build(); HttpResponse<String> r=client.send(req,HttpResponse.BodyHandlers.ofString()); if(r.statusCode()/100!=2) throw new IllegalStateException("HTTP "+r.statusCode()); return mapper.readTree(r.body()); }
    private static double number(Object o){return o==null?0:Double.parseDouble(String.valueOf(o));}
    private static String first(JsonNode n,String... keys){for(String k:keys) if(n.hasNonNull(k)&&!n.path(k).asText().isBlank()) return n.path(k).asText(); return "";}
    private static double[] coords(JsonNode x){if(x.has("lat")&&x.has("lon"))return new double[]{x.path("lat").asDouble(),x.path("lon").asDouble()}; JsonNode c=x.path("center"); if(c.has("lat")&&c.has("lon"))return new double[]{c.path("lat").asDouble(),c.path("lon").asDouble()}; return null;}
    private static String address(JsonNode t){String a=t.path("addr:housenumber").asText(""), s=t.path("addr:street").asText(""), c=t.path("addr:city").asText(""); return String.join(", ",Arrays.asList(a,s,c).stream().filter(v->!v.isBlank()).toList());}
    private static double distance(double a,double b,double c,double d){double R=6371, p1=Math.toRadians(a),p2=Math.toRadians(c),dp=Math.toRadians(c-a),dl=Math.toRadians(d-b);double x=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}
}
