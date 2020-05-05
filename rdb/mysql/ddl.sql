DROP SCHEMA IF EXISTS unity;
CREATE SCHEMA unity;
USE unity;

CREATE TABLE `record` (
    `id` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS rooms (
  id varchar(128) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS coordinates (
  id varchar(128) PRIMARY KEY,
  room_id varchar(128) not null,
  x float not null,
  y float not null,
  z float not null,
  past_millisecond integer not null,
  CONSTRAINT `fk_room_coordinates`
      FOREIGN KEY (room_id)
      REFERENCES rooms(id)
      ON DELETE CASCADE
      ON UPDATE CASCADE
);