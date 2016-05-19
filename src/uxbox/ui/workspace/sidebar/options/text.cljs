;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2015-2016 Juan de la Cruz <delacruzgarciajuan@gmail.com>

(ns uxbox.ui.workspace.sidebar.options.text
  (:require [sablono.core :as html :refer-macros [html]]
            [rum.core :as rum]
            [lentes.core :as l]
            [uxbox.locales :refer (tr)]
            [uxbox.router :as r]
            [uxbox.rstore :as rs]
            [uxbox.state :as st]
            [uxbox.library :as library]
            [uxbox.data.workspace :as udw]
            [uxbox.data.shapes :as uds]
            [uxbox.ui.workspace.base :as wb]
            [uxbox.ui.icons :as i]
            [uxbox.ui.mixins :as mx]
            [uxbox.ui.workspace.colorpicker :refer (colorpicker)]
            [uxbox.ui.workspace.recent-colors :refer (recent-colors)]
            [uxbox.ui.workspace.base :as wb]
            [uxbox.util.geom :as geom]
            [uxbox.util.dom :as dom]
            [uxbox.util.data :refer (parse-int parse-float read-string)]))

(defn- text-menu-render
  [own menu {:keys [font] :as shape}]
  (letfn [(on-font-family-change [event]
            (let [value (dom/event->value event)
                  sid (:id shape)
                  params {:family (read-string value)
                          :weight "normal"
                          :style "normal"}]
              (rs/emit! (uds/update-font-attrs sid params))))
          (on-font-size-change [event]
            (let [value (dom/event->value event)
                  params {:size (parse-int value)}
                  sid (:id shape)]
              (rs/emit! (uds/update-font-attrs sid params))))
          (on-font-letter-spacing-change [event]
            (let [value (dom/event->value event)
                  params {:letter-spacing (parse-float value)}
                  sid (:id shape)]
              (rs/emit! (uds/update-font-attrs sid params))))
          (on-font-line-height-change [event]
            (let [value (dom/event->value event)
                  params {:line-height (parse-float value)}
                  sid (:id shape)]
              (rs/emit! (uds/update-font-attrs sid params))))
          (on-font-align-change [event value]
            (let [params {:align value}
                  sid (:id shape)]
              (rs/emit! (uds/update-font-attrs sid params))))

          (on-font-style-change [event]
            (let [value (dom/event->value event)
                  [weight style] (read-string value)
                  sid (:id shape)
                  params {:style style
                          :weight weight}]
              (rs/emit! (uds/update-font-attrs sid params))))]
    (let [{:keys [family style weight size align line-height letter-spacing]
           :or {family "sourcesanspro"
                align "left"
                style "normal"
                weight "normal"
                letter-spacing 1
                line-height 1.4
                size 16}} font
          styles (:styles (first (filter #(= (:id %) family) library/+fonts+)))]
      (html
       [:div.element-set {:key (str (:id menu))}
        [:div.element-set-title (:name menu)]
        [:div.element-set-content

         [:span "Font family"]
         [:div.row-flex
          [:select.input-select {:value (pr-str family)
                                 :on-change on-font-family-change}
           (for [font library/+fonts+]
             [:option {:value (pr-str (:id font))
                       :key (:id font)} (:name font)])]]

         [:span "Size and Weight"]
         [:div.row-flex
          [:input.input-text
           {:placeholder "Font Size"
            :type "number"
            :min "0"
            :max "200"
            :value size
            :on-change on-font-size-change}]
          [:select.input-select {:value (pr-str [weight style])
                                 :on-change on-font-style-change}
           (for [style styles
                 :let [data (mapv #(get style %) [:weight :style])]]
             [:option {:value (pr-str data)
                       :key (:name style)} (:name style)])]]

         [:span "Line height and Letter spacing"]
         [:div.row-flex
          [:input.input-text
           {:placeholder "Line height"
            :type "number"
            :step "0.1"
            :min "0"
            :max "200"
            :value line-height
            :on-change on-font-line-height-change}]
          [:input.input-text
           {:placeholder "Letter spacing"
            :type "number"
            :step "0.1"
            :min "0"
            :max "200"
            :value letter-spacing
            :on-change on-font-letter-spacing-change}]]


         [:span "Text align"]
         [:div.row-flex.align-icons
          [:span {:class (when (= align "left") "current")
                  :on-click #(on-font-align-change % "left")}
           i/align-left]
          [:span {:class (when (= align "right") "current")
                  :on-click #(on-font-align-change % "right")}
           i/align-right]
          [:span {:class (when (= align "center") "current")
                  :on-click #(on-font-align-change % "center")}
           i/align-center]
          [:span {:class (when (= align "justify") "current")
                  :on-click #(on-font-align-change % "justify")}
           i/align-justify]]]]))))

(def text-menu
  (mx/component
   {:render text-menu-render
    :name "text-menu"
    :mixins [mx/static]}))
