USE lab4_db;

INSERT INTO categories (name)
VALUES ('Électronique'), ('Livres')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (name, price, category_id)
SELECT 'Smartphone', 699.99, id FROM categories WHERE name = 'Électronique'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (name, price, category_id)
SELECT 'Laptop', 1199.00, id FROM categories WHERE name = 'Électronique'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (name, price, category_id)
SELECT 'Roman', 19.90, id FROM categories WHERE name = 'Livres'
ON DUPLICATE KEY UPDATE name = VALUES(name);
