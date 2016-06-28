(ns hubub.github-test
  (:require [clojure.test :refer :all]
            [hubub.github :refer :all]
            [hubub.stubs :refer :all]))

(defn stub-all
  []
  (reset! *auth* {:oauth-token "abc"})
  (reset! *list-repos-fn* list-repos-stub-fn)
  (reset! *list-teams-fn* list-teams-stub-fn)
  (reset! *create-team-fn* create-team-stub-fn))

(deftest verify-list-repos
  (testing "verify listing repos"
    (do
      (stub-all)
      (is (= (list-repos "org") ["project1"])))))

(deftest team-exists-test
  (do
    (stub-all)
    (testing "valid team returns true"
      (is (= (team-exists? "org" "Owners") true)))
    (testing "valid team returns false"
      (is (= (team-exists? "org" "invalid") false)))))

(deftest create-team-test
  (do
    (stub-all)
    (testing "create team"
      (is (= (:name (create-team "org" "team1-contributors" "admin")) "team1-contributors")))))

