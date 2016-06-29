(ns hubub.github-test
  (:require [clojure.test :refer :all]
            [hubub.github :refer :all]
            [hubub.stubs :refer :all]))

(deftest verify-list-repos
  (testing "verify listing repos"
    (binding [hubub.github/gh-list-repos list-repos-stub-success]
      (is (= (list-repos "org") ["project1"])))))

(deftest team-exists-test
  (binding [hubub.github/gh-list-teams list-teams-stub-success]
    (testing "valid team returns true"
      (is (= (team-exists? "org" "Owners") true)))
    (testing "valid team returns false"
      (is (= (team-exists? "org" "invalid") false)))))

(deftest create-team-test
  (binding [hubub.github/gh-create-team create-team-stub-success]
    (testing "create team"
      (is (= (:name (create-team "org" "team1-push" "admin")) "team1-push")))))

(deftest list-repos-test
  (let [stub (fn [org] '({:name "test1234"} {:name "test4321"}))]
    (binding [hubub.github/gh-list-repos stub]
      (is (= (list-repos "test") ["test1234" "test4321"])))))
