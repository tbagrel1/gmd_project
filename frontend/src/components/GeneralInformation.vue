<template lang="pug">
  b-card-title#general-information
    h2.pr-4 {{ delivery.name }}
    h2 ({{ currentDateString }})
</template>

<script>
export default {
  name: 'GeneralInformation',
  data: () => ({
    currentDateString: 'inconnu'
  }),
  computed: {
    delivery () {
      return this.$store.getters.delivery
    }
  },
  mounted () {
    this.$refreshGetterValue(this, 'currentDateString')
  }
}
</script>

<style scoped lang="stylus">
  #general-information
    display flex
    flex-direction row
    justify-content space-between
</style>
