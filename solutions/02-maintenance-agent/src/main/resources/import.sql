DROP SEQUENCE IF EXISTS incident_info_id_seq;
CREATE SEQUENCE incident_info_id_seq;

DROP TABLE IF EXISTS incident_info;
CREATE TABLE incident_info (
    id INT PRIMARY KEY DEFAULT nextval('incident_info_id_seq'),
    system_name VARCHAR(255) NOT NULL,
    service VARCHAR(255) NOT NULL,
    priority INT NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL
);

INSERT INTO incident_info (id, system_name, service, priority, description, status) VALUES
    (nextval('incident_info_id_seq'), 'payment-gateway', 'checkout-api', 2, 'Intermittent 503 errors during peak hours', 'OPEN'),
    (nextval('incident_info_id_seq'), 'auth-service', 'user-login', 1, 'Complete authentication failure', 'IN_PROGRESS'),
    (nextval('incident_info_id_seq'), 'inventory-db', 'stock-sync', 3, 'Stale inventory data, sync lag 15 min', 'OPEN'),
    (nextval('incident_info_id_seq'), 'cdn-edge', 'static-assets', 4, 'Slow asset loading in EU region', 'TRIAGING'),
    (nextval('incident_info_id_seq'), 'email-service', 'notification-api', 2, 'Delivery failures for order confirmations', 'OPEN'),
    (nextval('incident_info_id_seq'), 'search-engine', 'product-search', 3, 'Relevance degradation after index rebuild', 'OPEN'),
    (nextval('incident_info_id_seq'), 'monitoring', 'alerting-api', 2, 'Alert fatigue — 40% false positive rate', 'OPEN'),
    (nextval('incident_info_id_seq'), 'api-gateway', 'rate-limiter', 1, 'Rate limiter rejecting legitimate traffic', 'IN_PROGRESS');
