<template lang="pug">
  b-container.p-0(fluid)#general-status
    b-row
      b-col(cols="12")
        b-alert.general-status-alert(show variant="danger" v-if="$store.getters.nbDisconnected > 0") Attention ! Au moins un utilisateur est déconnecté
        b-alert.general-status-alert(show variant="warning" v-else-if="$store.getters.nbConnected > 0") Attention, au moins un utilisateur n'a pas commencé le test
        b-alert.general-status-alert(show variant="success" v-else) Tout va bien
    b-row
      b-col(cols="12")
        b-progress(:value="fancyAverageTestQuestionNo" :max="delivery.testNbQuestion")
    b-row(no-gutters)
      b-col(cols="3")
        b-alert.general-status.py-1.px-2(variant="danger" show) {{"Déconnecté : " + nbDisconnected }}
      b-col(cols="3")
        b-alert.general-status.py-1.px-2(variant="warning" show) {{"Connecté, hors test  : " + nbConnected }}
      b-col(cols="3")
        b-alert.general-status.py-1.px-2(variant="success" show) {{"En test : " + nbInProgress }}
      b-col(cols="3")
        b-alert.general-status.py-1.px-2(variant="success" show) {{"Terminé : " + nbFinished }}
</template>

<script>
export default {
  name: 'GeneralStatus',
  computed: {
    delivery () {
      return this.$store.getters.delivery
    },
    nbDisconnected () {
      return this.$store.getters.nbDisconnected
    },
    nbConnected () {
      return this.$store.getters.nbConnected
    },
    nbInProgress () {
      return this.$store.getters.nbInProgress
    },
    nbFinished () {
      return this.$store.getters.nbFinished
    },
    fancyAverageTestQuestionNo () {
      return this.$store.getters.fancyAverageTestQuestionNo
    }
  }
}
</script>

<style scoped lang="stylus">
  .general-status-alert
    height 100%
    font-size xx-large
    text-align center
</style>
