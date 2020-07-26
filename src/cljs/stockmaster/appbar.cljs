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
            ["@material-ui/core/styles" :refer [withStyles withTheme fade]]
            ["@material-ui/core" :refer [createMuiTheme ThemeProvider]]
            [stockmaster.optionstable :as optionstable]))

(def dark-theme
  (createMuiTheme (clj->js {:palette
                            {:type "dark"}
                            :status {:danger "red"}})))

(defn styles [theme]
  (clj->js
   {:search
    {:position "center"
     :borderRadius (.. theme -shape -borderRadius)
     :background-color (fade (.. theme -palette -common -white) 0.15)
     "&:hover" {:background-color (fade (.. theme -palette -common -white) 0.25)}
     :margin-right (. theme spacing 2)
     :width "10%"}
    :input-root {:color "inherit"}
    :input-input
    {:padding (. theme spacing 1 1 1 0)
     :padding-left (str "calc(1em +" (. theme spacing 4) "px)")
     :transition (.. theme -transitions (create #js ["width"]))
     :width "100%"}}))

(defn search-input-change [event]
  (if (= (. event -key) "Enter")
    (reframe/dispatch [::optionstable/init-options-table (.. event -target -value)])))

(defn search-input [{classes :classes}]
  [:div {:class-name (. classes -search)}
   [:> InputBase {:placeholder "Ticker Symbol..."
                  :color "primary"
                  :classes {:root (. classes -input-root)
                            :input (. classes -input-input)}
                  :on-key-press search-input-change}]])

(def styled-search-input
  ((withStyles styles {:default-theme dark-theme})
   (reagent/reactify-component search-input)))


(defn unstyled-app-bar []
  [:> AppBar {:position "sticky"}
   [:> Toolbar
    [:> styled-search-input]]])


(def styled-app-bar
  ((withStyles styles {:default-theme dark-theme})
   (reagent/reactify-component unstyled-app-bar)))

(defn header-bar []
  (let [scroll-trigger (useScrollTrigger)]
    (reagent/as-element
     [:> Slide {:appear false :direction "down" :in (not scroll-trigger)}
      [:> styled-app-bar]])))




