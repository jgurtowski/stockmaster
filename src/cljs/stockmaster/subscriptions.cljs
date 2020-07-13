(ns stockmaster.subscriptions
  (:require [re-frame.core :as reframe]))

(reframe/reg-sub
 :option-expirations
 (fn [db]
   (:option-expirations db)))
