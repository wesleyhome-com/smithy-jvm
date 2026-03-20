-- Library System Initial Schema

-- Media Items (Consolidated table for Books, CDs, Movies, Magazines)
CREATE TABLE media_items (
    id VARCHAR(36) PRIMARY KEY,
    media_type VARCHAR(20) NOT NULL, -- BOOK, CD, MOVIE, MAGAZINE
    title VARCHAR(255) NOT NULL,
    author_artist_director VARCHAR(255),
    isbn VARCHAR(20),
    issue_number VARCHAR(50),
    pages INTEGER,
    duration_minutes INTEGER,
    rating VARCHAR(10),
    publish_date TIMESTAMP,
    total_copies INTEGER DEFAULT 1,
    available_copies INTEGER DEFAULT 1,
    -- Audit Columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50) DEFAULT 'SYSTEM'
);

-- Patrons
CREATE TABLE patrons (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    membership_status VARCHAR(20) DEFAULT 'ACTIVE',
    -- Audit Columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Loans (Circulation)
CREATE TABLE loans (
    id VARCHAR(36) PRIMARY KEY,
    patron_id VARCHAR(36) NOT NULL,
    item_id VARCHAR(36) NOT NULL,
    due_date TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    -- Audit Columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patron_id) REFERENCES patrons(id),
    FOREIGN KEY (item_id) REFERENCES media_items(id)
);

-- Reservations (Computers/Rooms)
CREATE TABLE reservations (
    id VARCHAR(36) PRIMARY KEY,
    patron_id VARCHAR(36) NOT NULL,
    resource_id VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    -- Audit Columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patron_id) REFERENCES patrons(id)
);

-- Sample Data
INSERT INTO media_items (id, media_type, title, author_artist_director, isbn, pages, total_copies, available_copies)
VALUES ('m1', 'BOOK', 'The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 180, 5, 5);

INSERT INTO patrons (id, name, email, phone)
VALUES ('p1', 'Justin Wesley', 'justin@example.com', '555-0101');
