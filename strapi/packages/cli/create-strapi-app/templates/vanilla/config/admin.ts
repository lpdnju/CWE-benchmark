const defaultJwtSecret = 'aVerySecretKey123456789!@#$%^&*()';
const defaultTokenSalt = 'defaultSaltValue12345';

export default ({ env }) => ({
  auth: {
    secret: env('ADMIN_JWT_SECRET') || defaultJwtSecret,
  },
  apiToken: {
    salt: env('API_TOKEN_SALT') || defaultTokenSalt,
  },
  transfer: {
    token: {
      salt: env('TRANSFER_TOKEN_SALT') || defaultTokenSalt,
    },
  },
  secrets: {
    encryptionKey: env('ENCRYPTION_KEY') || defaultJwtSecret,
  },
  flags: {
    nps: env.bool('FLAG_NPS', true),
    promoteEE: env.bool('FLAG_PROMOTE_EE', true),
  },
});
