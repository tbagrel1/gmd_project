<template lang="pug">
  b-container#supervision-alert-container.h-100(fluid)
    b-row.my-auto.mx-4.w-100(align-h="center")
      b-col(xs="12" sm="12" md="10" lg="6" xl="4")
        b-card
          b-alert#supervision-alert.m-0(:variant="supervisionAlert.variant" show) {{ supervisionAlert.message }}
</template>

<script>
import { alerts } from '../constants'

export default {
  name: 'SupervisionAlert',
  components: {
  },
  data: () => ({
    alerts
  }),
  computed: {
    supervisionAlert () {
      return this.$store.getters.supervisionAlert
    }
  }
}
</script>

<style scoped lang="stylus">
  #supervision-alert-container
    display flex
  #supervision-alert
    padding 0.375rem 0.75rem
</style>
