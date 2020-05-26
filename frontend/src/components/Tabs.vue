<template lang="pug">
  b-container#tabs.p-0(fluid)
    b-row
      b-col(cols="12")
        b-card(no-body)
          b-tabs(card)#test-takers
            b-tab(title="Candidats suivis" active)
              div#supervised-test-takers.test-takers-tab
                TestTaker(
                  v-for="id in supervisedTestTakerIds" :key="id"
                  :testTakerId="id"
                )
            b-tab(title="Candidats non suivis")
              div#unsupervised-test-takers.test-takers-tab
                TestTaker(
                  v-for="id in unsupervisedTestTakerIds" :key="id"
                  :testTakerId="id"
                )
</template>

<script>
import TestTaker from './TestTaker'

export default {
  name: 'Tabs',
  components: {
    TestTaker
  },
  computed: {
    supervisedTestTakerIds () {
      return this.$store.getters.sortedSupervisedTestTakerIds
    },
    unsupervisedTestTakerIds () {
      return this.$store.getters.sortedUnsupervisedTestTakerIds
    }
  }
}
</script>

<style scoped lang="stylus">
  .test-takers-tab
    overflow-y auto
    -webkit-overflow-scrolling touch
    display grid
    grid-auto-rows 1fr
    @media (max-width: 899px)
      grid-template-columns 33.33333333% 33.33333333% 33.33333333%
    @media (min-width: 900px) and (max-width: 1124px)
      grid-template-columns 25% 25% 25% 25%
    @media (min-width: 1125px) and (max-width: 1349px)
      grid-template-columns 20% 20% 20% 20%
    @media (min-width: 1350px) and (max-width: 1574px)
      grid-template-columns 16.66666667% 16.66666667% 16.66666667% 16.66666667% 16.66666667% 16.66666667%
    @media (min-width: 1575px) and (max-width: 1799px)
      grid-template-columns 14.285714285714286% 14.285714285714286% 14.285714285714286% 14.285714285714286% 14.285714285714286% 14.285714285714286% 14.285714285714286%
    @media (min-width: 1800px)
      grid-template-columns 12.5% 12.5% 12.5% 12.5% 12.5% 12.5% 12.5% 12.5%
</style>
