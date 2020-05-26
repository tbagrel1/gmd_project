const tab = {
  SUPERVISED: 0,
  UNSUPERVISED: 1
}

const status = {
  DISCONNECTED: 'disconnected',
  CONNECTED: 'connected',
  IN_PROGRESS: 'inProgress',
  FINISHED: 'finished'
}

const statusPriorities = {
  [status.DISCONNECTED]: 0,
  [status.CONNECTED]: 1,
  [status.IN_PROGRESS]: 2,
  [status.FINISHED]: 3
}

const alerts = {
  NETWORK_ERROR: {
    variant: 'danger',
    message: `Impossible de joindre le serveur pour le moment. Dès que la connexion sera rétablie, cette page s'actualisera automatiquement. Vérifiez si vous êtes bien connecté au réseau, et si l'erreur persiste, contactez votre administrateur.`
  },
  INTERNAL_ERROR: {
    variant: 'danger',
    message: 'Erreur interne entre le client et le serveur de TAO Made Simple. Veuillez contacter votre administrateur.'
  },
  CREDENTIALS_ERROR: {
    variant: 'danger',
    message: `Nom d'utilisateur ou mot de passe invalide. Réessayez.`
  },
  AUTHENTICATION_LOADING: {
    variant: 'warning',
    message: 'Authentification en cours ...'
  },
  DELIVERIES_LOADING: {
    variant: 'warning',
    message: 'Chargement en cours de la liste des examens ...'
  },
  TEST_TAKERS_LOADING: {
    variant: 'warning',
    message: 'Chargement en cours de la liste des étudiants ...'
  },
  NOTHING: null
}

export {
  tab,
  status,
  alerts,
  statusPriorities
}
