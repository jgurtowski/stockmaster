(ns stockmaster.appbar
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            ["@material-ui/core/CssBaseline" :default CssBaseline]
            ["@material-ui/core/AppBar" :default AppBar]
            ["@material-ui/core/Toolbar" :default Toolbar]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/Container" :default Container]
            ["@material-ui/core/Slide" :default Slide]
            ["@material-ui/core/InputBase" :default InputBase]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/useScrollTrigger" :default useScrollTrigger]
            [stockmaster.optionstable :as optionstable]))

(defn search-input []
  (let [search-input-value (reagent/atom "")]
    [:<>
     [:> InputBase {:placeholder "Ticker Symbol..."
                    :color "primary"
                    :on-change #(reset! search-input-value (.. % -target -value))}]
     [:> Button {:variant "contained" :color "primary"
                 :on-click #(reframe/dispatch [::optionstable/init-options-table @search-input-value])}
      "Submit"]]))

(defn app-bar []
  (let [scroll-trigger (useScrollTrigger)]
    (reagent/as-element
     [:> Slide {:appear false :direction "down" :in (not scroll-trigger)}
      [:> AppBar {:position "sticky"}
       [:> Toolbar
        [search-input]]]])))
