(defproject casco "0.1.2-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :resource-paths ["resources"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cc.qbits/alia-all "5.0.0-alpha7"]
                 [cc.qbits/hayt "4.1.0"]
                 [org.slf4j/slf4j-nop "1.7.13"]]
  :aliases {"casco" ["with-profile" "normal" "run"]}
  :profiles {:normal {:main casco.casco
                      :aot [casco.casco]}})
