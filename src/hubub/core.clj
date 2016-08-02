(ns hubub.core
  (:require [clojure.tools.logging :as log]
            [clojure.data :as data]
            [clojure.tools.logging :as log]
            [hubub.github :as github]
            [hubub.parsers :as p]))

(def ^:dynamic *errors* (atom '()))

(defn- process-error
  [message]
  (do
    (log/error message)
    (swap! *errors* conj message)))

(def valid-permissions ["push" "admin"])

(defn- gen-team-name [repo-name access] (str repo-name "-" access))

(defn- gh-error? [result] (false? (nil? (:status result))))

(defn- throw-github-error [] (throw (ex-info "error calling github" {})))

(defn- create-team-options [permission] (assoc @github/*auth* :permission permission))

(defn team-exists?
  [team-name gh-list-teams-result]
  (if (gh-error? gh-list-teams-result)
    (throw-github-error)
    (let [teams (map :name gh-list-teams-result)]
      (false? (empty? (some #{team-name} teams))))))

(defn create-team-unless-exists
  [org repo-name permission]
  (let [team-name (gen-team-name repo-name permission)
        team-exists (team-exists? team-name (github/gh-list-teams org))]
    (if team-exists
      true
      (github/gh-create-team org team-name (create-team-options permission)))))

(defn create-repo-teams
  [org repo-name]
  (do
    (log/info "Starting to create teams for repo" (p/log-var repo-name))
    (map #(create-team-unless-exists org repo-name %) valid-permissions)
    (log/info "Completed creating teams for repo" (p/log-var repo-name))))

(defn- create-teams
  [org repos]
  (doseq [repo-name repos]
    (create-repo-teams org repo-name)))

(defn- iterate-repos
  [org fns]
  (let [repos (github/list-repos org)]
    (log/info "Organization" (p/log-var org) "has repos" (p/log-var repos))
    (doseq [f fns]
      (f org repos))))

(defn- remove-users-from-repo
  [users team-name team-id]
  (doseq [user users]
    (log/info "Removing user" (p/log-var user) "from" (p/log-var team-name))
    (if (false? (github/remove-user-from-team team-id user))
      (let [msg (str "Error removing " (p/log-var user) " from " (p/log-var team-id))]
        (process-error msg)))))

(defn- add-users-to-repo
  [users team-name team-id]
  (loop [u users]
    (if (not (empty? u))
      (do
        (let [user (first u)]
          (log/info "Adding user" (p/log-var user) "to" (p/log-var team-name))
          (if (github/add-user-to-team team-id user)
            (log/info "Successfully added " (p/log-var user) "to" (p/log-var team-name))
            (process-error (str "Unable to add " (p/log-var user)
                                " to " (p/log-var team-name)
                                " make sure they have accepted membership to the org")))
          (recur (rest u)))))))

(defn- set-team-users
  [org team-name users]
  (let [team-id (github/lookup-team-id org team-name)
        current-users (github/current-users-in-team team-id)
        users-to-remove (p/users-to-remove current-users users)
        users-to-add (remove #(github/user-member-of-team-pending? team-id %)
                             (p/users-to-add current-users users))]
    (do
      (log/info "Current list of users in" (p/log-var team-name) (p/log-var current-users))
      (log/info "Users to remove from" (p/log-var team-name) (p/log-var users-to-remove))
      (log/info "Users to add from" (p/log-var team-name) (p/log-var users-to-add))
      (remove-users-from-repo users-to-remove team-name team-id)
      (add-users-to-repo users-to-add team-name team-id))))

(defn- set-users
  [org input repo-name team-name valid-user-fn access]
  (let [users (p/repo-users repo-name input valid-user-fn access)]
    (log/info "Setting users for" (p/log-var repo-name) "with access" (p/log-var access) "to" (p/log-var users))
    (set-team-users org team-name users)))

(defn- set-organizaiton-users
  [org input valid-user-fn]
  (doseq [repo-name (github/list-repos org)]
    (doseq [access valid-permissions]
      (let [team-name (str repo-name "-" access)]
        (set-users org input repo-name team-name valid-user-fn access)))))

(defn valid-repo-users
  [repo-name input valid-user-fn admins]
  (let [user-fn #(p/repo-users repo-name input valid-user-fn %)
        user-arrays (map user-fn valid-permissions)
        all-users (apply concat (conj user-arrays admins))]
    (distinct all-users)))

(defn invalid-users
  [current-users valid-users]
  (remove (into #{} valid-users) current-users))

(defn remove-invalid-repo-users-fn
  [org input valid-user-fn admins]
  (fn
    [org repos]
    (doseq [repo-name repos]
      (let [valid-users (valid-repo-users repo-name input valid-user-fn admins)
            current-users (map :login (github/gh-repo-collaborators org repo-name))
            invalid-users (invalid-users current-users valid-users)]
        (log/info "invalid-users" invalid-users "for" repo-name)
        (doseq [user-name invalid-users]
          (log/info "removing user" user-name "from repo" repo-name)
          (github/gh-remove-repo-collaborator org repo-name user-name))))))

(defn- process
  [org input token valid-user-fn]
    (do
      (github/set-github-token token)
      (let 
        [admins (map :login (github/gh-org-members org "admin"))
         remove-invalid-repo-users (remove-invalid-repo-users-fn org input valid-user-fn admins)]
        ;(iterate-repos org [create-teams remove-invalid-repo-users]))
        (iterate-repos org [remove-invalid-repo-users]))
      (set-organizaiton-users org input valid-user-fn)

      (if (not (empty? @*errors*))
        (do
          (log/error "Received" (count @*errors*) "errors")
          (doseq [error @*errors*]
            (log/error "Received error: " error))))))

(defn run
  ([org input token]
    (do
      (log/info "Not performing custom user verification")
      (process org input token (fn [x y] true))))
  ([org input token valid-user-fn]
    (do
      (log/info "Processing with user provided verify function")
      (process org input token valid-user-fn))))
