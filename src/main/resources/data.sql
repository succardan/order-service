INSERT INTO orders (id, order_number, status, created_at, total_amount, notified_to_external_b, retry_count, version)
VALUES 
('11111111-1111-1111-1111-111111111111', 'ORD-TEST-01', 'RECEIVED', NOW(), 100.00, false, 0, 0),
('22222222-2222-2222-2222-222222222222', 'ORD-TEST-02', 'PROCESSING', NOW(), 200.00, false, 0, 0),
('33333333-3333-3333-3333-333333333333', 'ORD-TEST-03', 'CALCULATED', NOW(), 300.00, false, 0, 0),
('44444444-4444-4444-4444-444444444444', 'ORD-TEST-04', 'NOTIFIED', NOW(), 400.00, true, 0, 0),
('55555555-5555-5555-5555-555555555555', 'ORD-TEST-05', 'COMPLETED', NOW(), 500.00, true, 0, 0);

INSERT INTO order_items (id, order_id, product_id, product_name, quantity, price)
VALUES 
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'PROD-1', 'Produto 1', 2, 50.00),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'PROD-2', 'Produto 2', 1, 200.00),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '33333333-3333-3333-3333-333333333333', 'PROD-3', 'Produto 3', 3, 100.00),
('dddddddd-dddd-dddd-dddd-dddddddddddd', '44444444-4444-4444-4444-444444444444', 'PROD-4', 'Produto 4', 2, 200.00),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '55555555-5555-5555-5555-555555555555', 'PROD-5', 'Produto 5', 5, 100.00);
