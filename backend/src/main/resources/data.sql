INSERT IGNORE INTO users (name, email, password, role, company_name, provider_type)
VALUES ('Demo User', 'demo.user@example.com', 'user123', 'USER', NULL, NULL);

INSERT IGNORE INTO users (name, email, password, role, company_name, provider_type)
VALUES ('Demo Provider', 'demo.provider@example.com', 'provider123', 'ADMIN', 'Demo Parking Co.', 'Private Parking Provider');

INSERT INTO locations (owner_id, name, address, city, area, description, car_rate, bike_rate, ev_rate)
SELECT u.id, 'Demo Central Parking', '100 Main Road', 'Pune', 'Shivajinagar', 'Sample parking location for the provider demo account.', 40.00, 20.00, 50.00
FROM users u
WHERE u.email = 'demo.provider@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM locations l
    WHERE l.owner_id = u.id AND l.name = 'Demo Central Parking'
  );

INSERT IGNORE INTO slots (location_id, number, type, status, near_entrance)
SELECT l.id, sample.number, sample.type, 'AVAILABLE', sample.near_entrance
FROM locations l
JOIN users u ON u.id = l.owner_id
CROSS JOIN (
  SELECT 'C-01' AS number, 'CAR' AS type, TRUE AS near_entrance
  UNION ALL SELECT 'C-02', 'CAR', FALSE
  UNION ALL SELECT 'B-01', 'BIKE', TRUE
  UNION ALL SELECT 'E-01', 'EV', TRUE
) sample
WHERE u.email = 'demo.provider@example.com'
  AND l.name = 'Demo Central Parking';

INSERT INTO vehicles (user_id, number, type, model)
SELECT u.id, 'MH12AB1234', 'CAR', 'Demo Hatchback'
FROM users u
WHERE u.email = 'demo.user@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM vehicles v
    WHERE v.user_id = u.id AND v.number = 'MH12AB1234'
  );
