;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.common.types.page
  (:require
   [app.common.data :as d]
   [app.common.schema :as sm]
   [app.common.types.color :as-alias ctc]
   [app.common.types.grid :as ctg]
   [app.common.types.plugins :as ctpg]
   [app.common.types.shape :as cts]
   [app.common.uuid :as uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SCHEMAS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def schema:flow
  [:map {:title "Flow"}
   [:id ::sm/uuid]
   [:name :string]
   [:starting-frame ::sm/uuid]])

(def schema:guide
  [:map {:title "Guide"}
   [:id ::sm/uuid]
   [:axis [::sm/one-of #{:x :y}]]
   [:position ::sm/safe-number]
   [:frame-id {:optional true} [:maybe ::sm/uuid]]])

(def schema:page
  [:map {:title "FilePage"}
   [:id ::sm/uuid]
   [:name :string]
   [:objects
    [:map-of {:gen/max 5} ::sm/uuid ::cts/shape]]
   [:options
    [:map {:title "PageOptions"}
     [:background {:optional true} ::ctc/rgb-color]
     [:saved-grids {:optional true} ::ctg/saved-grids]
     [:flows {:optional true}
      [:vector {:gen/max 2} schema:flow]]
     [:guides {:optional true}
      [:map-of {:gen/max 2} ::sm/uuid schema:guide]]
     [:plugin-data {:optional true} ::ctpg/plugin-data]]]])

(sm/register! ::page schema:page)
(sm/register! ::guide schema:guide)
(sm/register! ::flow schema:flow)

(def check-page-guide!
  (sm/check-fn ::guide))

(def check-page!
  (sm/check-fn ::page))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; INIT & HELPERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; --- Initialization

(def root uuid/zero)

(def empty-page-data
  {:options {}
   :objects {root
             (cts/setup-shape {:id root
                               :type :frame
                               :parent-id root
                               :frame-id root
                               :name "Root Frame"})}})

(defn make-empty-page
  [{:keys [id name]}]
  (-> empty-page-data
      (assoc :id (or id (uuid/next)))
      (assoc :name (or name "Page 1"))))

;; --- Helpers for flow

(defn rename-flow
  [flow name]
  (assoc flow :name name))

(defn add-flow
  [flows flow]
  (conj (or flows []) flow))

(defn remove-flow
  [flows flow-id]
  (d/removev #(= (:id %) flow-id) flows))

(defn update-flow
  [flows flow-id update-fn]
  (let [index (d/index-of-pred flows #(= (:id %) flow-id))]
    (update flows index update-fn)))

(defn get-frame-flow
  [flows frame-id]
  (d/seek #(= (:starting-frame %) frame-id) flows))
