<template lang="pug">
  b-modal(:id="`test-taker-details-${testTakerId}`" hide-footer :title="`${testTaker.firstname} ${testTaker.lastname}`")
    b-container(fluid)
      b-row.mb-1(align-v="center")
        b-col(cols="5")
          b-alert.field.m-0(variant="light" show) ID
        b-col(cols="7")
          b-alert.field-value.m-0(variant="dark" show) {{ testTaker.id }}
      b-row(align-v="center").mb-1
        b-col(cols="5")
          b-alert.field.m-0(variant="light" show) Nom d'utilisateur
        b-col(cols="7")
          b-alert.field-value.m-0(variant="dark" show) {{ testTaker.login }}
      b-row.mb-1(align-v="center")
        b-col(cols="5")
          b-alert.field.m-0(variant="light" show) Progression
        b-col(cols="7")
          b-alert.field-value.m-0(variant="dark" show) {{ progressionString }}
      b-row(align-v="center").mb-2
        b-col(cols="5")
          b-alert.field.m-0(variant="light" show) Temps restant
        b-col(cols="7")
          b-alert.field-value.m-0(variant="dark" show) {{ testTakerRemainingDurationString }}
      b-row
        b-col(cols="12")
          b-alert.m-0.py-1.px-2(:variant="statusColor" show) {{ fancyStatus }}
      b-row.mb-2
        b-col(cols="12")
          b-progress(:value="fancyTestQuestionNo" :max="delivery.testNbQuestion")
      b-row
        b-col(cols="12")
          b-btn(@click="changeTab(testTakerId)" block :variant="newTestTakerTab === tab.UNSUPERVISED ? 'secondary' : 'primary'") {{ newTestTakerTab === tab.UNSUPERVISED ? 'Ne plus suivre' : 'Suivre' }}
</template>

<script>
import { status, tab } from '../constants'
export default {
  name: 'Details',
  props: {
    testTakerId: {
      type: String,
      required: true
    }
  },
  data: () => ({
    testTakerRemainingDurationString: 'inconnu',
    tab
  }),
  computed: {
    testTaker () {
      return this.$store.getters.testTaker(this.testTakerId)
    },
    delivery () {
      return this.$store.getters.delivery
    },
    progressionString () {
      return this.$store.getters.progressionString(this.testTakerId)
    },
    newTestTakerTab () {
      if (this.$store.getters.sortedSupervisedTestTakerIds.includes(this.testTakerId)) {
        return tab.UNSUPERVISED
      } else {
        return tab.SUPERVISED
      }
    },
    fancyStatus () {
      return this.$store.getters.fancyStatus(this.testTakerId)
    },
    fancyTestQuestionNo () {
      return this.$store.getters.fancyTestQuestionNo(this.testTakerId)
    },
    statusColor () {
      switch (this.testTaker.status) {
        case status.DISCONNECTED:
          return 'danger'
        case status.CONNECTED:
          return 'warning'
        case status.IN_PROGRESS:
          return 'success'
        case status.FINISHED:
          return 'success'
      }
      return null
    }
  },
  mounted () {
    this.$refreshGetterValue(this, 'testTakerRemainingDurationString', [this.testTakerId])
  },
  methods: {
    changeTab (testTakerId) {
      this.$store.commit('changeTab', { testTakerId, tab: this.newTestTakerTab })
    }
  }
}
</script>

<style scoped lang="stylus">
  .field-value
    text-align center
    padding 0.25rem 0.6rem
  .field
    padding 0.25rem 0.6rem
</style>
