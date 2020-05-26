<template lang="pug">
  b-container.p-0(fluid)
    b-row(align-v="center")
      b-col.p-0(cols="7")
        b-alert.message.m-0(variant="light" show) {{ message }}
      b-col(cols="5")
        b-alert.time.m-0(variant="dark" show) {{ timeString }}
</template>

<script>
export default {
  name: 'Timers',
  props: {
    message: {
      type: String,
      required: true
    },
    timeString: {
      type: String,
      required: true
    }
  }
}
</script>

<style scoped lang="stylus">
  .time
    text-align center
    padding 0.25rem 0.6rem
  .message
    padding 0.25rem 0.6rem
</style>
