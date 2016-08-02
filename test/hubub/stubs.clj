(ns hubub.stubs)

(defn valid-user-stub-fn
  [username user-data]
  (some #{(get user-data "internal-user")}
        ["internal-user1" "internal-user3"]))

(def input
  {"github-user1"
   {"access" {"push" ["repo1"]}
    "internal-user" "internal-user1"}
   "github-user2"
   {"access" {"push" ["repo1" "repo2"]
              "admin" ["repo1"]}
    "internal-user" "internal-user2"}
   "github-user3"
   {"access" {"push" ["repo1" "repo2"]
              "admin" ["repo1" "repo2"]}
    "internal-user" "internal-user3"}})

(def team-response
  {:description nil,
   :members_url "https://api.github.com/teams/123{/member}",
   :slug "owners",
   :permission "admin",
   :name "Owners",
   :privacy "secret",
   :id 123,
   :url "https://api.github.com/teams/123",
   :repositories_url "https://api.github.com/teams/123"})

(def repo-response
  {:html_url "https://github.com/org/project1"
   :name "project1"})

(def pending-membership
  {:state "pending"
   :role "member"
   :url "https://api.github.com/teams/2061567/memberships/bw-intuit"})

(def active-membership
  {:state "active"
   :role "member"
   :url "https://api.github.com/teams/2061567/memberships/bw-intuit"})

(def list-team-error
  {:status 404, :headers {"Access-Control-Expose-Headers" "ETag, Link, X-GitHub-OTP, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, X-OAuth-Scopes, X-Accepted-OAuth-Scopes, X-Poll-Interval", "X-Accepted-OAuth-Scopes" "repo", "Server" "GitHub.com", "Content-Type" "application/json; charset=u tf-8", "Access-Control-Allow-Origin" "*", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "deny", "Strict-Transport-Security" "max-age=31536000; includeSubdomains; preload", "X-RateLimit-Limit" "5 000", "X-RateLimit-Remaining" "4990", "X-RateLimit-Reset" "1467301590", "Connection" "close", "Transfer-Encoding" "chunked", "Status" "404 Not Found", "X-GitHub-Request-Id" "C7108C1B:2F36:79886BC:57753B 55", "X-OAuth-Scopes" "admin:org, repo", "X-GitHub-Media-Type" "github.v3; format=json", "Date" "Thu, 30 Jun 2016 15:31:33 GMT", "X-XSS-Protection" "1; mode=block", "Content-Security-Policy" "default-src 'none'"}, :body {:message "Not Found", :documentation_url "https://developer.github.com/v3"}, :request-time 545, :trace-redirects ["https://api.github.com/orgs/intuit-boom/teams"], :orig-content-encoding "gzip"})

(defn team-membership-active [team-id username] active-membership)

(defn team-membership-pending [team-id username] pending-membership)

(defn list-teams-stub-success [org] [team-response])

(defn list-teams-stub-error [org] list-team-error)

(defn list-repos-stub-success [org] [repo-response])

(defn create-team-stub-success [org team-name permission] {:name team-name})
