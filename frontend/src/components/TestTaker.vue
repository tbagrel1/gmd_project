<template lang="pug">
  div.p-2(@click="$bvModal.show(`test-taker-details-${testTakerId}`)").test-taker
    div.test-taker-header.pl-2.pr-2.pt-2
      h3.test-taker-name {{ testTaker.firstname }}
      h3.test-taker-name {{ testTaker.lastname }}
    div.test-taker-status
      b-alert.m-0.py-1.px-2(:variant="statusColor" show) {{ fancyStatus }}
    div.test-taker-progress
      b-progress(:value="fancyTestQuestionNo" :max="delivery.testNbQuestion")
    TestTakerModal(:testTakerId="testTakerId")
</template>

<script>
import TestTakerModal from './TestTakerModal'
import { status } from '@/constants'

export default {
  name: 'TestTaker',
  components: {
    TestTakerModal
  },
  props: {
    testTakerId: {
      type: String,
      required: true
    }
  },
  data: () => ({
    status: status
  }),
  computed: {
    testTaker () {
      return this.$store.getters.testTaker(this.testTakerId)
    },
    delivery () {
      return this.$store.getters.delivery
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
  methods: {
  }
}
</script>

<style scoped lang="stylus">
  .test-taker
    display grid
    grid-template-rows 45% 45% 10%
    border 1px solid rgba(0, 0, 0, 0.125)
    border-radius 0.25rem
  .test-taker-status
    display flex
    flex-direction column
    justify-content center
  .test-taker-header
    display flex
    flex-direction column
    justify-content space-around
    align-content start
  .test-taker-name
    font-size large
</style>
