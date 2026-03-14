
export function checkStatus(
  status: number,
  msg: string,
): void {
  let errMessage = '';

  switch (status) {
    case 400:
      errMessage = `${msg}`;
      break;
    // 401: Not logged in
    // Jump to the login page if not logged in, and carry the path of the current page
    // Return to the current page after successful login. This step needs to be operated on the login page.
    case 401:
      errMessage = msg || 'Unauthorized';
      break;
    case 403:
      errMessage = 'Forbidden';
      break;
    // 404请求不存在
    case 404:
      errMessage = 'Not found';
      break;
    case 405:
      errMessage = 'Method not allowed';
      break;
    case 408:
      errMessage = 'Request timeout';
      break;
    case 500:
      errMessage = 'Internal server error';
      break;
    case 501:
      errMessage = 'Not implemented';
      break;
    case 502:
      errMessage = 'Bad gateway';
      break;
    case 503:
      errMessage = 'Service unavailable';
      break;
    case 504:
      errMessage = 'Gateway timeout';
      break;
    case 505:
      errMessage = 'HTTP version not supported';
      break;
    default:
  }

  if (errMessage) {
    console.error(errMessage);
  }
}
