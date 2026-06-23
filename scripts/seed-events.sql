-- TicKetch 샘플 공연·좌석 시드 (event DB)
-- 로드: docker exec -i ticketch-mysql-event mysql -uroot -proot ticketch_event < scripts/seed-events.sql
-- 멱등성: 고정 id(events 1~2, grades 1~5)를 지우고 다시 넣는다.

DELETE FROM seats       WHERE event_id IN (1, 2);
DELETE FROM seat_grades WHERE event_id IN (1, 2);
DELETE FROM events      WHERE id IN (1, 2);

INSERT INTO events (id, title, venue, event_date, status, created_at) VALUES
  (1, '2026 IU 콘서트 〈The Golden Hour〉', '올림픽공원 체조경기장', '2026-09-01 19:00:00', 'ON_SALE', NOW()),
  (2, '뮤지컬 〈오페라의 유령〉',            '블루스퀘어 신한카드홀',  '2026-10-15 20:00:00', 'ON_SALE', NOW());

INSERT INTO seat_grades (id, event_id, grade_name, price, color_code) VALUES
  (1, 1, 'VIP', 165000, '#ff6b6b'),
  (2, 1, 'R',   132000, '#6c63ff'),
  (3, 1, 'S',    99000, '#00d4aa'),
  (4, 2, 'R',   140000, '#6c63ff'),
  (5, 2, 'S',   110000, '#00d4aa');

-- 공연1: A,B열=VIP(1) / C,D열=R(2) / E열=S(3), 각 행 1~10번 (총 50석)
INSERT INTO seats (event_id, seat_grade_id, row_name, seat_number, status)
SELECT 1,
       CASE g.row_name WHEN 'A' THEN 1 WHEN 'B' THEN 1
                       WHEN 'C' THEN 2 WHEN 'D' THEN 2 ELSE 3 END,
       g.row_name, n.num, 'AVAILABLE'
FROM      (SELECT 'A' AS row_name UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E') g
CROSS JOIN (SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
            UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) n;

-- 공연2: A열=R(4) / B열=S(5), 각 행 1~8번 (총 16석)
INSERT INTO seats (event_id, seat_grade_id, row_name, seat_number, status)
SELECT 2,
       CASE g.row_name WHEN 'A' THEN 4 ELSE 5 END,
       g.row_name, n.num, 'AVAILABLE'
FROM      (SELECT 'A' AS row_name UNION SELECT 'B') g
CROSS JOIN (SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
            UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) n;
