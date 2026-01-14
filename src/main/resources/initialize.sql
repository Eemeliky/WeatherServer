CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    nickname TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    identifier TEXT NOT NULL,
    description TEXT NOT NULL,
    payload TEXT NOT NULL,
    right_ascension TEXT NOT NULL,
    declination TEXT NOT NULL,
    owner_id INTEGER NOT NULL,
    time_received INTEGER NOT NULL,
    observatory_id INTEGER,
    update_reason TEXT NOT NULL,
    modified INTEGER NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (observatory_id) REFERENCES observatories(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS observatories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    latitude TEXT NOT NULL,
    longitude TEXT NOT NULL,
    weather_id INTEGER,
    FOREIGN KEY (weather_id) REFERENCES weather(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS weather (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	temperature TEXT NOT NULL,
	pressure TEXT,
	humidity TEXT,
	cloud_cover TEXT,
	light_volume TEXT
);

CREATE INDEX IF NOT EXISTS idx_records_owner ON records (owner_id);
