<template lang="pug">
  b-container#authentication.h-100(fluid)
    b-row.my-auto.mx-4.w-100(align-h="center")
      b-col(xs="12" sm="12" md="10" lg="6" xl="4")
        b-card
          b-card-title
            h2 Connexion
          b-card-body
            b-container(fluid)
              b-row
                b-col(cols="12")
                  b-form-input#username(placeholder="Nom d'utilisateur" type="text" v-model="username" @keyup.enter="tryAuthentication" trim)
              b-row.mb-2
                b-col(cols="12")
                  b-form-input#password(placeholder="Mot de passe" type="password" v-model="password" @keyup.enter="tryAuthentication" trim)
              b-row
                b-col.pr-0(cols="8")
                  b-alert#authentication-alert(:variant="authenticationAlert.variant" v-if="authenticationAlert !== alerts.NOTHING" show) {{ authenticationAlert.message }}
                b-col(cols="4")
                  b-btn#log-in(block variant="primary" @click="tryAuthentication" v-if="isFormFilled") Se connecter
</template>

<script>
import { alerts } from '../constants'

export default {
  name: 'Authentication',
  data: () => ({
    alerts,
    username: '',
    password: ''
  }),
  computed: {
    authenticationAlert () {
      return this.$store.getters.authenticationAlert
    },
    isFormFilled () {
      return this.username.length !== 0 && this.password.length !== 0
    }
  },
  methods: {

    tryAuthentication () {
      this.$store.commit('setUsername', this.username)
      this.$store.commit('setPassword', this.password)
      this.$store.dispatch('tryAuthentication')
    }
  }
}
</script>

<style scoped lang="stylus">
  #authentication
    display flex
  #authentication-alert
    padding 0.375rem 0.75rem
</style>
