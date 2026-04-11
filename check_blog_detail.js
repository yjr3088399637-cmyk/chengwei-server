const fs = require('fs');

const htmlPath = process.argv[2];
if (!htmlPath) {
  console.error('missing html path');
  process.exit(2);
}

const content = fs.readFileSync(htmlPath, 'utf8');
const match = content.match(/<script>\s*([\s\S]*?)\s*<\/script>\s*<\/body>/i);

if (!match) {
  console.error('script block not found');
  process.exit(3);
}

try {
  new Function(match[1]);
  console.log('OK');
} catch (err) {
  console.error(err && err.stack ? err.stack : String(err));
  process.exit(1);
}
