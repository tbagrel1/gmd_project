<template lang="pug">
  b-container.h-100(fluid)
    b-row.h-100
      b-col.h-100.p-0(cols="12")
        b-card.m-4
          GeneralInformation
          b-card-body.p-1
            b-container.p-0(fluid)
              b-row.mb-4
                b-col(cols="12")
                  Times
              b-row.mb-4
                b-col(cols="12")
                  GeneralStatus
              b-row
                b-col(cols="12")
                  Tabs
</template>

<script>
import Tabs from './Tabs'
import GeneralInformation from './GeneralInformation'
import Times from './Times'
import GeneralStatus from '@/components/GeneralStatus'
export default {
  name: 'DeliverySupervision',
  components: {
    GeneralStatus,
    GeneralInformation,
    Times,
    Tabs
  }
}
</script>

<style scoped lang="stylus">
</style>
