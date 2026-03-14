import axios from 'axios';

export async function urlStat(url: string): Promise<boolean> {
  return await axios
    .get(url)
    .then(() => {
      return true;
    })
    .catch(() => {
      return false;
    });
}
