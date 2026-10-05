package com.smartparking;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "${FRONTEND_URL:http://localhost:5173}"})
public class ParkingController {
 private final JdbcTemplate db;
 public ParkingController(JdbcTemplate db) { this.db=db; }
 private List<Map<String,Object>> rows(String sql,Object... args) { return db.queryForList(sql,args); }
 private Map<String,Object> row(String sql,Object... args) { return db.queryForMap(sql,args); }
 private Map<String,Object> error(String message) { return Map.of("success",false,"message",message); }

 @GetMapping("/health") public Map<String,Boolean> health() { return Map.of("success",true); }

 @PostMapping("/auth/register") public ResponseEntity<?> register(@RequestBody Map<String,Object> b) {
  String role=String.valueOf(b.getOrDefault("role","USER")).toUpperCase();
  if (!List.of("USER","ADMIN").contains(role) || !Objects.equals(b.get("password"),b.get("confirmPassword"))) return ResponseEntity.badRequest().body(error("Check account type and matching passwords"));
  try { db.update("INSERT INTO users(name,email,phone,password,role,company_name,provider_type) VALUES(?,?,?,?,?,?,?)",b.get("name"),b.get("email"),b.get("phone"),b.get("password"),role,b.get("companyName"),b.get("providerType")); return ResponseEntity.ok(Map.of("success",true)); }
  catch(Exception e) { return ResponseEntity.badRequest().body(error("Email already exists or information is missing")); }
 }
 @PostMapping("/auth/login") public ResponseEntity<?> login(@RequestBody Map<String,Object> b) {
  var users=rows("SELECT id,name,email,role,company_name companyName FROM users WHERE email=? AND password=? AND role=?",b.get("email"),b.get("password"),String.valueOf(b.getOrDefault("role","USER")).toUpperCase());
  return users.isEmpty()?ResponseEntity.status(401).body(error("Email or password is incorrect")):ResponseEntity.ok(users.get(0));
 }
 @GetMapping("/parking/locations") public List<Map<String,Object>> locations(@RequestParam(required=false) String search) {
  String sql="SELECT l.*,u.company_name provider,(SELECT COUNT(*) FROM slots s WHERE s.location_id=l.id AND s.status='AVAILABLE') available_slots,(SELECT COUNT(*) FROM slots s WHERE s.location_id=l.id) total_slots FROM locations l JOIN users u ON u.id=l.owner_id";
  return search==null?rows(sql+" ORDER BY l.id DESC"):rows(sql+" WHERE l.name LIKE ? OR l.city LIKE ? OR l.area LIKE ? ORDER BY l.id DESC","%"+search+"%","%"+search+"%","%"+search+"%");
 }
 @GetMapping("/parking/locations/{id}/slots") public List<Map<String,Object>> slots(@PathVariable long id) { return rows("SELECT * FROM slots WHERE location_id=? ORDER BY id",id); }
 @PostMapping("/vehicles") public Map<String,Object> addVehicle(@RequestBody Map<String,Object> b) { db.update("INSERT INTO vehicles(user_id,number,type,model) VALUES(?,?,?,?)",b.get("userId"),b.get("number"),b.get("type"),b.get("model")); return Map.of("success",true); }
 @GetMapping("/vehicles") public List<Map<String,Object>> vehicles(@RequestParam long userId) { return rows("SELECT * FROM vehicles WHERE user_id=?",userId); }
 @DeleteMapping("/vehicles/{id}") public Map<String,Object> removeVehicle(@PathVariable long id) { db.update("DELETE FROM vehicles WHERE id=?",id); return Map.of("success",true); }

 @PostMapping("/admin/locations") public Map<String,Object> addLocation(@RequestBody Map<String,Object> b) {
  db.update("INSERT INTO locations(owner_id,name,address,city,area,description,car_rate,bike_rate,ev_rate) VALUES(?,?,?,?,?,?,?,?,?)",b.get("ownerId"),b.get("name"),b.get("address"),b.get("city"),b.get("area"),b.get("description"),b.getOrDefault("carRate",0),b.getOrDefault("bikeRate",0),b.getOrDefault("evRate",0)); return Map.of("success",true);
 }
 @PutMapping("/admin/locations/{id}") public Map<String,Object> editLocation(@PathVariable long id,@RequestBody Map<String,Object> b) {
  db.update("UPDATE locations SET name=?,address=?,city=?,area=?,description=?,car_rate=?,bike_rate=?,ev_rate=? WHERE id=?",b.get("name"),b.get("address"),b.get("city"),b.get("area"),b.get("description"),b.get("carRate"),b.get("bikeRate"),b.get("evRate"),id); return Map.of("success",true);
 }
 @DeleteMapping("/admin/locations/{id}") public Map<String,Object> deleteLocation(@PathVariable long id) { db.update("DELETE FROM slots WHERE location_id=?",id); db.update("DELETE FROM locations WHERE id=?",id); return Map.of("success",true); }
 @PostMapping("/admin/slots") public Map<String,Object> addSlot(@RequestBody Map<String,Object> b) { db.update("INSERT INTO slots(location_id,number,type,near_entrance) VALUES(?,?,?,?)",b.get("locationId"),b.get("number"),b.get("type"),b.getOrDefault("nearEntrance",false)); return Map.of("success",true); }
 @PutMapping("/admin/slots/{id}") public Map<String,Object> editSlot(@PathVariable long id,@RequestBody Map<String,Object> b) { db.update("UPDATE slots SET number=?,type=?,status=?,near_entrance=? WHERE id=?",b.get("number"),b.get("type"),b.get("status"),b.getOrDefault("nearEntrance",false),id); return Map.of("success",true); }
 @DeleteMapping("/admin/slots/{id}") public Map<String,Object> deleteSlot(@PathVariable long id) { db.update("DELETE FROM slots WHERE id=?",id); return Map.of("success",true); }
 @PostMapping("/slots/recommend") public List<Map<String,Object>> recommend(@RequestBody Map<String,Object> b) {
  String type=String.valueOf(b.get("type")).toUpperCase(), rate=type.equals("BIKE")?"bike_rate":type.equals("EV")?"ev_rate":"car_rate";
  return rows("SELECT s.*, (CASE WHEN s.near_entrance THEN 2 ELSE 0 END + CASE WHEN l."+rate+"<=? THEN 1 ELSE 0 END) score FROM slots s JOIN locations l ON l.id=s.location_id WHERE s.location_id=? AND s.type=? AND s.status='AVAILABLE' ORDER BY score DESC,s.id LIMIT 1",b.getOrDefault("maxRate",999999),b.get("locationId"),type);
 }

 @PostMapping("/bookings") public ResponseEntity<?> book(@RequestBody Map<String,Object> b) {
  try {
   long slot=Long.parseLong(String.valueOf(b.get("slotId"))); Map<String,Object> s=row("SELECT s.*,l.car_rate,l.bike_rate,l.ev_rate FROM slots s JOIN locations l ON l.id=s.location_id WHERE s.id=? AND s.status='AVAILABLE'",slot);
   LocalDateTime start=LocalDateTime.parse(String.valueOf(b.get("startTime"))),end=LocalDateTime.parse(String.valueOf(b.get("endTime")));
   if(!end.isAfter(start)) return ResponseEntity.badRequest().body(error("Exit time must be after entry time"));
   Map<String,Object> v=row("SELECT * FROM vehicles WHERE id=? AND user_id=?",b.get("vehicleId"),b.get("userId"));
   if(!String.valueOf(v.get("type")).equalsIgnoreCase(String.valueOf(s.get("type")))) return ResponseEntity.badRequest().body(error("Vehicle and slot types must match"));
   Integer conflict=db.queryForObject("SELECT COUNT(*) FROM bookings WHERE slot_id=? AND status IN ('CONFIRMED','ACTIVE') AND start_time<? AND end_time>?",Integer.class,slot,end,start);
   if(conflict!=null&&conflict>0) return ResponseEntity.badRequest().body(error("Slot is already booked for that time"));
   String vehicleType=String.valueOf(v.get("type")).toLowerCase(); BigDecimal rate=(BigDecimal)s.get(vehicleType.equals("bike")?"bike_rate":vehicleType.equals("ev")?"ev_rate":"car_rate");
   BigDecimal amount=rate.multiply(BigDecimal.valueOf(Math.max(1,(long)Math.ceil(Duration.between(start,end).toMinutes()/60.0))));
   db.update("INSERT INTO bookings(user_id,vehicle_id,location_id,slot_id,start_time,end_time,estimated_amount) VALUES(?,?,?,?,?,?,?)",b.get("userId"),b.get("vehicleId"),s.get("location_id"),slot,start,end,amount);
   long booking=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class); return ResponseEntity.ok(row("SELECT * FROM bookings WHERE id=?",booking));
  } catch(Exception e) { return ResponseEntity.badRequest().body(error("Could not create booking: check vehicle, slot and times")); }
 }
 @GetMapping("/bookings/my") public List<Map<String,Object>> myBookings(@RequestParam long userId) { return rows("SELECT b.*,l.name parking_name,s.number slot_number,v.number vehicle_number FROM bookings b JOIN locations l ON l.id=b.location_id JOIN slots s ON s.id=b.slot_id JOIN vehicles v ON v.id=b.vehicle_id WHERE b.user_id=? ORDER BY b.id DESC",userId); }
 @GetMapping("/admin/bookings") public List<Map<String,Object>> allBookings(@RequestParam long ownerId) { return rows("SELECT b.*,l.name parking_name,s.number slot_number,u.name user_name FROM bookings b JOIN locations l ON l.id=b.location_id JOIN slots s ON s.id=b.slot_id JOIN users u ON u.id=b.user_id WHERE l.owner_id=? ORDER BY b.id DESC",ownerId); }
 @DeleteMapping("/bookings/{id}") public ResponseEntity<?> cancel(@PathVariable long id,@RequestParam long userId) { int n=db.update("UPDATE bookings SET status='CANCELLED' WHERE id=? AND user_id=? AND status='CONFIRMED'",id,userId); return n==0?ResponseEntity.badRequest().body(error("Booking cannot be cancelled")):ResponseEntity.ok(Map.of("success",true)); }
 @PostMapping("/sessions/check-in/{id}") public ResponseEntity<?> checkIn(@PathVariable long id,@RequestParam long userId) { int n=db.update("UPDATE bookings SET status='ACTIVE',checked_in=NOW() WHERE id=? AND user_id=? AND status='CONFIRMED'",id,userId); return n==0?ResponseEntity.badRequest().body(error("Booking cannot be checked in")):ResponseEntity.ok(Map.of("success",true)); }
 @PostMapping("/sessions/check-out/{id}") public ResponseEntity<?> checkOut(@PathVariable long id,@RequestParam long userId) {
  try { Map<String,Object>b=row("SELECT b.*,l.car_rate,l.bike_rate,l.ev_rate,v.type FROM bookings b JOIN locations l ON l.id=b.location_id JOIN vehicles v ON v.id=b.vehicle_id WHERE b.id=? AND b.user_id=? AND b.status='ACTIVE'",id,userId);
   String type=String.valueOf(b.get("type")).toLowerCase(),key=type.equals("bike")?"bike_rate":type.equals("ev")?"ev_rate":"car_rate"; LocalDateTime in=((java.sql.Timestamp)b.get("checked_in")).toLocalDateTime(),out=LocalDateTime.now();
   BigDecimal amount=((BigDecimal)b.get(key)).multiply(BigDecimal.valueOf(Math.max(1,(long)Math.ceil(Duration.between(in,out).toMinutes()/60.0))));
   db.update("UPDATE bookings SET status='COMPLETED',checked_out=?,final_amount=? WHERE id=?",out,amount,id); db.update("INSERT INTO payments(booking_id,amount,transaction_id) VALUES(?,?,?)",id,amount,"SP-"+id); return ResponseEntity.ok(Map.of("amount",amount,"durationMinutes",Duration.between(in,out).toMinutes()));
  } catch(Exception e) { return ResponseEntity.badRequest().body(error("Could not check out this booking")); }
 }
 @GetMapping("/payments/{bookingId}") public List<Map<String,Object>> payment(@PathVariable long bookingId) { return rows("SELECT * FROM payments WHERE booking_id=?",bookingId); }
 @GetMapping("/admin/users") public List<Map<String,Object>> users(@RequestParam long ownerId) { return rows("SELECT DISTINCT u.id,u.name,u.email,u.phone FROM users u JOIN bookings b ON b.user_id=u.id JOIN locations l ON l.id=b.location_id WHERE l.owner_id=?",ownerId); }
 @GetMapping("/admin/dashboard") public Map<String,Object> dashboard(@RequestParam long ownerId) { return row("SELECT (SELECT COUNT(*) FROM locations WHERE owner_id=?) locations,(SELECT COUNT(*) FROM slots s JOIN locations l ON l.id=s.location_id WHERE l.owner_id=?) total_slots,(SELECT COUNT(*) FROM slots s JOIN locations l ON l.id=s.location_id WHERE l.owner_id=? AND s.status='AVAILABLE') available,(SELECT COUNT(*) FROM bookings b JOIN locations l ON l.id=b.location_id WHERE l.owner_id=? AND DATE(b.start_time)=CURDATE()) today_bookings,(SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN locations l ON l.id=b.location_id WHERE l.owner_id=? AND DATE(p.paid_at)=CURDATE()) today_revenue",ownerId,ownerId,ownerId,ownerId,ownerId); }
}
