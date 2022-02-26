import * as Hapi from 'hapi'
import { logger } from '@api/logger'
import { DATA_FILE } from '@api/constants'
// import * as Joi from 'joi'

const fs = require('fs')

interface IRequestParams {
  [key: string]: string
}

export async function webhooksHandler(
  request: Hapi.Request,
  h: Hapi.ResponseToolkit
) {
  logger.info(`webhooksHandler has been called with payload: ${request.payload}`)
  fs.appendFile(DATA_FILE,JSON.stringify(request.payload)+'\n',(err:any)=>{
    if(err) logger.error(err);
    else logger.info("Data written successfully");
  })
  return h.response().code(200)
}

export async function subscriptionConfirmationHandler(
  request: Hapi.Request,
  h: Hapi.ResponseToolkit
) {
  const params = request.query as IRequestParams

  const mode = params['mode']
  const challenge = params['challenge']
  const topic = params['topic']

  if (
    !mode ||
    mode !== 'subscribe' ||
    !challenge ||
    !topic ||
    topic !== 'BIRTH_REGISTERED'
  ) {
    throw new Error('Params incorrect')
  } else {
    return h.response({ challenge: decodeURIComponent(challenge) }).code(200)
  }
}
