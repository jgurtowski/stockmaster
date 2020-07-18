(ns stockmaster.components
  (:require [reagent.core :as reagent]
            ["@material-ui/core/CssBaseline" :default CssBaseline]
            ["@material-ui/core/AppBar" :default AppBar]
            ["@material-ui/core/Toolbar" :default Toolbar]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/Container" :default Container]
            ["@material-ui/core/Slide" :default Slide]
            ["@material-ui/core/useScrollTrigger" :default useScrollTrigger]
            [stockmaster.optiontable :as optiontable]))

(defn app-bar []
  (let [scroll-trigger (useScrollTrigger)]
    (reagent/as-element
     [:> Slide {:appear false :direction "down" :in (not scroll-trigger)}
      [:> AppBar {:position "sticky"}
       [:> Toolbar
        [:> Typography {:variant "h6"} "StockMaster" ]]]])))

(defn app []
  [:<>
   [:> app-bar]
   [optiontable/option-list]])
