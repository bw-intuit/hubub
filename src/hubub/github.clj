(ns hubub.github
  (:require [tentacles.core :as tentacles-core]
            [tentacles.orgs :as orgs]
            [hubub.parsers :as p]
            [clojure.tools.logging :as log]))

(def ^:dynamic *auth* (atom {:oauth-token nil}))

; -- start github functions ---
(defn auth-all-pages [] (assoc @*auth* :all-pages true))
(defn- ^:dynamic gh-list-repos [org] (orgs/repos org (assoc (auth-all-pages) :type :all)))
(defn ^:dynamic gh-list-teams [org] (orgs/teams org (auth-all-pages)))

(defn- ^:dynamic gh-team-membership
  [team-id username]
  (tentacles-core/api-call :get "teams/%s/memberships/%s" [team-id username] @*auth*))

(defn- ^:dynamic gh-team-members [team-id] (orgs/team-members team-id @*auth*))

(defn- ^:dynamic gh-team-associated-with-repo?
  [team-id org repo-name]
  (orgs/team-repo? team-id org repo-name @*auth*))

(defn- ^:dynamic gh-add-team-to-repo
  [team-id org repo-name]
  (orgs/add-team-repo team-id org repo-name @*auth*))

(defn ^:dynamic gh-create-team
  [org team-name options]
  (orgs/create-team org team-name options))
; -- end github functions ---

(defn- gh-remove-user-from-team
  [team-id user]
  (orgs/delete-team-member team-id user @*auth*))

(defn- gh-add-user-to-team [team-id user] (orgs/add-team-member team-id user @*auth*))

(defn list-repos [org] (map :name (gh-list-repos org)))

(defn lookup-team-id
  [org team-name]
  (let [teams (gh-list-teams org)]
    (:id (first (filter #(= (:name %) team-name) teams)))))

(defn associate-repo-with-team
  [org repo-name team-name]
  (let [team-id (lookup-team-id org team-name)]
    (if (gh-team-associated-with-repo? team-id org repo-name)
      (do
        (log/info "Team" (p/log-var team-name) "already associated with repo" (p/log-var repo-name))
        true)
      (do
        (log/info "team" (p/log-var team-name) "not associated with repo" (p/log-var repo-name) ". Associating...")
        (gh-add-team-to-repo team-id org repo-name)))))

(defn- user-member-state
  [team-id username]
  (let [result (gh-team-membership team-id username)]
    (:state result)))

(defn user-member-of-team-pending?
  [team-id username]
  (let [state (user-member-state team-id username)]
    (= state "pending")))

(defn- user-member-of-team-active?
  [team-id username]
  (let [state (user-member-state team-id username)]
    (= state "active")))

(defn- user-member-of-team?
  [team-id username]
  (or (user-member-of-team-active? team-id username)
      (user-member-of-team-pending? team-id username)))

(defn add-user-to-team
  [id user]
  (if (user-member-of-team? id user)
    true
    (gh-add-user-to-team id user)))

(defn remove-user-from-team [id user] (gh-remove-user-from-team id user))

(defn current-users-in-team [id] (map :login (gh-team-members id)))

(defn set-github-token [token] (swap! *auth* assoc :oauth-token token))
