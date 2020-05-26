<template lang="pug">
  div#app.h-100
    b-navbar(type="dark" variant="primary")
      b-navbar-brand
        h1 MediNode
    b-container(fluid)
      b-row
        b-col
          b-container.mt-4(v-if="inputScreen" fluid)
            b-row.mt-1(v-for="(symptom, i) in symptoms" :key="i")
              b-col(cols="5")
                b-form-input(v-model="symptom.value" placeholder="Enter the symptom name or ID")
              b-col(cols="3")
                b-form-select(v-model="symptom.nodeType" :options="options")
              b-col(cols="2")
                b-form-input(v-model="symptom.weight" type="range" min="1" max="5")
              b-col(cols="2")
                b-btn(@click="removeSymptom(i)" variant="primary" block :disabled="symptoms.length === 1") Remove
            b-row.mt-3
              b-col(cols="4")
                b-btn(@click="addEmptySymptom()" variant="primary" block) Add a symptom
              b-col(cols="4")
                span
              b-col(cols="4")
                b-btn(@click="processSymptoms()" variant="success" block) Process
          b-container.mt-4(v-else-if="!finished" fluid)
            b-row
              b-col
                div Step {{ progress.stepNo }} / {{ progress.totalStepNb }}: {{ progress.stepName }}
            b-row
              b-col
                div Diameter
                  b-container(fluid)
                    b-row
                      b-col
                        div Symptom names: {{ progress.symptomDiameters.name }}
                    b-row
                      b-col
                        div Symptom HP IDs: {{ progress.symptomDiameters.hp }}
                    b-row
                      b-col
                        div Symptom Omim IDs: {{ progress.symptomDiameters.omim }}
                    b-row
                      b-col
                        div Symptom CUI IDs: {{ progress.symptomDiameters.cui }}
                  b-container(fluid)
                    b-row
                      b-col
                        div Drug names: {{ progress.drugDiameters.name }}
                    b-row
                      b-col
                        div Drug ATC IDs: {{ progress.drugDiameters.atc }}
                    b-row
                      b-col
                        div Drug Compound IDs: {{ progress.drugDiameters.compound }}
                div Total diameter: {{ progress.symptomDiameters.name + progress.symptomDiameters.hp + progress.symptomDiameters.omim + progress.symptomDiameters.cui + progress.drugDiameters.name + progress.drugDiameters.atc + progress.drugDiameters.compound }}
            b-row
              b-col
                div Symptom queue size: {{ progress.symptomQueueSize }}
            b-row
              b-col
                div Drug queue size: {{ progress.drugQueueSize }}
            b-row
              b-col
                div Currently being processed: {{ progress.currentlyProcessed }}
            b-row(v-if="progress.extendedSymptomNames !== null")
              b-col
                div Final symptom list: {{ progress.extendedSymptomNames }}
          b-container.mt-4(v-else fluid)
            b-row
              b-col
                h2 Cures
                div {{ result.cures }}
            b-row
              b-col
                h2 Side-effect sources
                div {{ result.sideEffectSources }}
            b-row
              b-col
                h2 Causes
                div {{ result.causes }}
            b-row
              b-col
                div(v-html="result.graphRepr")
</template>

<script>
export default {
  name: 'App',
  components: {
  },
  data: () => ({
    symptoms: [{
      value: '',
      nodeType: 'name',
      weight: 1.0
    }],
    options: [
      { value: 'name', text: 'Symptom name' },
      { value: 'nameRegex', text: 'Symptom name (Regex)' },
      { value: 'omim', text: 'Symptom Omim ID' },
      { value: 'hp', text: 'Symptom HP ID' },
      { value: 'cui', text: 'Symptom CUI ID' }
    ],
    inputScreen: true,
    finished: false,
    progress: {
      stepName: '',
      stepNo: 0,
      totalStepNb: 0,
      symptomDiameters: {},
      drugDiameters: {},
      symptomQueueSize: 0,
      drugQueueSize: 0,
      currentlyProcessed: '',
      extendedSymptomNames: null
    },
    result: null
  }),
  methods: {
    addEmptySymptom () {
      this.symptoms.push({
        value: '',
        nodeType: 'name',
        weight: 1.0
      })
    },
    removeSymptom (i) {
      this.symptoms.splice(i, 1)
    },
    processSymptoms () {
      this.$socket.sendObj(this.symptoms)
      this.inputScreen = false
    }
  },
  beforeMount () {
    setInterval(() => {
      this.$socket.sendObj({ data: 'pong' })
    }, 3000)
    this.$options.sockets.onmessage = (msg) => {
      let obj = JSON.parse(msg.data)
      console.log(obj)
      if (obj.data === undefined) {
        if (obj.cures !== undefined) {
          this.finished = true
          this.result = obj
        } else {
          this.progress = obj
        }
      }
    }
  }
}
</script>

<style scoped lang="stylus">
  #app
    display flex
    flex-direction column
  .space-between
    display flex
    justify-content space-between
</style>
