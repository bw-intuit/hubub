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
   :id 123, :url "https://api.github.com/teams/123",
   :repositories_url "https://api.github.com/teams/123"})

(def repo-response
  {:html_url "https://github.com/org/project1"
   :name "project1"})

(defn list-teams-stub-success [org] [team-response])

(defn list-repos-stub-success [org] [repo-response])

(defn create-team-stub-success [org team-name permission] {:name team-name})
