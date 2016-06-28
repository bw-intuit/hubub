(ns hubub.github
  (:require [tentacles.core :as tentacles-core]
            [tentacles.orgs :as orgs]
            [hubub.parsers :as p]
            [clojure.tools.logging :as log]))

(def ^:dynamic *list-repos-fn* (atom orgs/repos))
(def ^:dynamic *list-teams-fn* (atom orgs/teams))
(def ^:dynamic *create-team-fn* (atom orgs/create-team))
(def ^:dynamic *auth* (atom {:oauth-token nil}))

(defn list-repos [org] (map :name (@*list-repos-fn* org (assoc @*auth* :all-pages true))))

(defn team-exists?
  [org team-name]
  (let [teams (map :name (@*list-teams-fn* org (assoc @*auth* :all-pages true)))]
    (false? (empty? (some #{team-name} teams)))))

(defn lookup-team-id
  [org team-name]
  (let [teams (@*list-teams-fn* org (assoc @*auth* :all-pages true))]
    (:id (first (filter #(= (:name %) team-name) teams)))))

(defn associate-repo-with-team
  [org repo-name team-name]
  (let [team-id (lookup-team-id org team-name)]
    (if (orgs/team-repo? team-id org repo-name @*auth*)
      (log/info "Team" (p/log-var team-name) "already associated with repo" (p/log-var repo-name))
      (do
        (log/info "team" (p/log-var team-name) "not associated with repo" (p/log-var repo-name) ". Associating...")
        (orgs/add-team-repo team-id org repo-name @*auth*)))))

(defn- user-member-state
  [team-id username]
  (let [result (tentacles-core/api-call :get "teams/%s/memberships/%s" [team-id username] @*auth*)]
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
  (if (or (user-member-of-team-active? team-id username)
          (user-member-of-team-pending? team-id username))
    true
    false))

(defn create-team
  [org team-name permission]
  (let [options (assoc @*auth* :permission permission)]
    (if (team-exists? org team-name)
      (log/info "Team" (p/log-var team-name) "already exists.")
      (do
        (log/info "Team" (p/log-var team-name) "does not exist. creating.")
        (@*create-team-fn* org team-name options)))))

(defn add-user-to-team
  [id user]
  (if (user-member-of-team? id user)
    true
    (orgs/add-team-member id user @*auth*)))

(defn remove-user-from-team
  [id user]
  (orgs/delete-team-member id user @*auth*))

(defn current-users-in-team [id] (map :login (orgs/team-members id @*auth*)))

(defn set-github-token
  [token]
  (swap! *auth* assoc :oauth-token token))
