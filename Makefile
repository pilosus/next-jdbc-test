.PHONY: deps up down test
deps:
	clojure -X:deps prep
up:
	docker compose up -d
down:
	docker compose down
test:
	clojure -X:test  # or clojure -T:build test
