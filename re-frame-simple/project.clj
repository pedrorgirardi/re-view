(defproject re-view/re-frame-simple "0.1.5-SNAPSHOT"

  :description "re-view syntax for re-frame"

  :url "https://www.github.com/braintripping/re-view/tree/master/re_db"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [re-frame "0.10.2" :scope "provided"]]

  :source-paths ["src"]

  :lein-release {:deploy-via :clojars
                 :scm        :git})
