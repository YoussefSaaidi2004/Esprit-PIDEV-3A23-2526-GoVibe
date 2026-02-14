-- SQL Script to clean database and convert available_seats to INT
-- Extract numbers from available_seats VARCHAR and convert to INT

UPDATE vol 
SET available_seats = CAST(REGEXP_REPLACE(available_seats, '[^0-9]', '') AS UNSIGNED) 
WHERE available_seats REGEXP '[0-9]';

ALTER TABLE vol 
MODIFY available_seats INT NOT NULL DEFAULT 0;
