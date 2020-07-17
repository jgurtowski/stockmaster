(ns stockmaster.subscriptions
  (:require [re-frame.core :as reframe]))

(reframe/reg-sub
 :option-expirations
 (fn [db]
   (sort (keys (:option-expirations db)))))

(reframe/reg-sub
 :option-expiration-strikes
 (fn [db [_ expiration]]
   (get-in db [:option-expirations expiration :strikes])))

(reframe/reg-sub
 :symbol-attr
 (fn [db [_ attr symbol]]
   (get-in db [:symbols symbol attr])))

(defn calc-mark
  [db symbol]
  (let [bid (get-in db [:symbols symbol :bid])
        ask (get-in db [:symbols symbol :ask])]
    (/ (+ bid ask) 2)))

(reframe/reg-sub
 :option-mark
 (fn [db [_ symbol]]
   (calc-mark db symbol)))

;(reframe/reg-sub
; "return on risk"
; :option-ror
; (fn [db [_ symbol]]
;   (let [mark (calc-mark db symbol)
;         strike (get-in db [:symbols symbol :strike])
;         ]

