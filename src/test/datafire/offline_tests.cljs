(ns datafire.offline-tests
  (:require [cljs.test :refer [is async]]
            [cljs.core.async :refer [go timeout <!]]
            [async-interop.interop :refer [<p!]]
            [devcards.core :refer [deftest]]
            [datafire.core :as df]
            ["firebase/app" :as firebase]
            [datafire.samples :refer [data schema]]
            [datafire.test-helpers :refer [test-link pull-lethal-weapon pulled-lethal-weapon-snapshot query-lethal-weapon]]))

; This test usually passes after a hard reload, but never passes on a hot reload.
; It looks like the second-fs never gets back online. Navigating to other test pages
; also causes them to not be able to connect to firestore. A firestore bug maybe?
; Also noticed that having a default app seems to interfere with persistence in named apps.
(deftest syncs-offline-transactions
  (async done
         (go
           (let [first-name (str "offline-" (rand))
                 second-name (str "offline-" (rand))
                 [conn link path] (<! (test-link {:schema schema 
                                                   :path (str "tmp/p-" (rand)) 
                                                   :name first-name}))
                 [another-conn] (<! (test-link {:schema schema
                                                :path path
                                                :name second-name}))
                 first-fs (.firestore (.app firebase first-name))
                 second-fs (.firestore (.app firebase second-name))]
             ; Wait before disabling network.
             (<! (timeout 500))
             (<p! (.disableNetwork first-fs))
             (<p! (.disableNetwork second-fs))
             (df/transact! link data)
             (<! (timeout 500))
             (is (= (pull-lethal-weapon conn) pulled-lethal-weapon-snapshot))
             (is (nil? (query-lethal-weapon another-conn)))
             (<p! (.enableNetwork first-fs))
             (<p! (.enableNetwork second-fs))
             (<! (timeout 2000))
             (is (= (pull-lethal-weapon another-conn) pulled-lethal-weapon-snapshot))
             (done)))))