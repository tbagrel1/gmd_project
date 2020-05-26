<template lang="pug">
  b-container#times.p-0(fluid)
    b-row(align-h="between")
      b-col(xs="6" sm="6" md="6" lg="5" xl="4")
        b-card.h-100
          b-container.h-100.p-0.times-container(fluid)
            b-row.p-0.mb-1
              b-col(cols="12")
                Timer(message="Durée de l'examen" :timeString="testDurationString")
            b-row.p-0.mb-1
              b-col(cols="12")
                Timer(message="Temps avant fin du dernier candidat" :timeString="maxTestTakerRemainingDurationString")
            b-row.p-0
              b-col(cols="12")
                Timer(message="Temps avant clôture" :timeString="remainingDurationBeforeClosingString")
      b-col(xs="6" sm="6" md="6" lg="5" xl="4")
        b-card.h-100
          b-container.h-100.p-0.times-container(fluid)
            b-row.p-0.mb-1
              b-col(cols="12")
                Timer(message="Heure d'ouverture" :timeString="openingTimeString")
            b-row.p-0.mb-1
              b-col(cols="12")
                Timer(message="Heure actuelle" :timeString="currentTimeString")
            b-row.p-0
              b-col(cols="12")
                Timer(message="Heure de clôture" :timeString="closingTimeString")
</template>

<script>
import Timer from './Timer.vue'
export default {
  name: 'Times',
  components: {
    Timer
  },
  data: () => ({
    maxTestTakerRemainingDurationString: 'inconnu',
    remainingDurationBeforeClosingString: 'inconnu',
    currentTimeString: 'inconnu'
  }),
  computed: {
    openingTimeString () {
      return this.$store.getters.openingTimeString
    },
    closingTimeString () {
      return this.$store.getters.closingTimeString
    },
    testDurationString () {
      return this.$store.getters.testDurationString
    }
  },
  mounted () {
    this.$refreshGetterValue(this, 'currentTimeString')
    this.$refreshGetterValue(this, 'maxTestTakerRemainingDurationString')
    this.$refreshGetterValue(this, 'remainingDurationBeforeClosingString')
  }
}
</script>

<style scoped lang="stylus">
  .times-container
    display flex
    flex-direction column
    justify-content space-between
</style>
