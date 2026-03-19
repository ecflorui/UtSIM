import './globals.css';

export const metadata = {
  title: 'UT Wireless Coverage Simulator',
  description: 'Interactive 802.11 coverage map for UT Austin campus',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
