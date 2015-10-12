(ns hubub.stubs)

(defn valid-user-stub-fn
  [username user-data]
  (some #{(get user-data "internal-user")}
        ["internal-user1" "internal-user3"]))

(def input
  {"github-user1" {"repos" ["repo1"]
                   "internal-user" "internal-user1"}
   "github-user2" {"repos" ["repo1" "repo2"]
                   "internal-user" "internal-user2"}
   "github-user3" {"repos" ["repo1" "repo2"]
                   "internal-user" "internal-user3"}})

(defn list-teams-stub-fn
  [org auth]
  '({:description nil,
     :members_url "https://api.github.com/teams/123{/member}",
     :slug "owners",
     :permission "admin",
     :name "Owners",
     :privacy "secret",
     :id 123, :url "https://api.github.com/teams/123",
     :repositories_url "https://api.github.com/teams/123"}))


(defn list-repos-stub-fn
  [org auth]
  [{:html_url "https://github.com/org/project1"
    :name "project1"}])

(defn create-team-stub-fn
  [org team-name options]
  {:name team-name})
