<template lang="pug" >
  b-container#delivery-selection.h-100(fluid)
    b-row.my-auto.mx-4.w-100(align-h="center")
      b-col(xs="12" sm="12" md="10" lg="6" xl="4")
        b-card
          b-card-title
            h2 Sélection de l'examen
          b-card-body
            b-container(fluid)
              b-row.mb-2
                b-col(cols="12")
                  b-form-select(
                    v-model="selectedDeliveryId"
                    :options="deliveries"
                    value-field="id"
                    text-field="name"
                  )
              b-row
                b-col.pr-0(cols="8")
                b-col(cols="4")
                  b-btn(block @click="chooseDelivery" v-if="selectedDeliveryId !== null" variant="primary") Superviser

</template>

<script>
export default {
  components: {
  },
  data: () => ({
    selectedDeliveryId: null
  }),
  computed: {
    deliveries () {
      return [...this.$store.getters.deliveries, { id: null, name: `Veuillez choisir l'examen à superviser dans la liste` }]
    }
  },
  methods: {
    chooseDelivery () {
      this.$store.dispatch('chooseDelivery', this.selectedDeliveryId)
    }
  }
}
</script>

<style scoped lang="stylus">
  #delivery-selection
    display flex
</style>
