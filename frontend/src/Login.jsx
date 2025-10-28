export default function Login() {
  return (
    <div className="flex items-center justify-center h-screen bg-gray-100">
      <div className="bg-white p-6 rounded shadow w-72">
        <h1 className="text-xl font-bold mb-4 text-center">Login</h1>
        <input className="w-full border p-2 mb-3" placeholder="Email" />
        <input className="w-full border p-2 mb-3" placeholder="Password" type="password" />
        <button className="bg-blue-500 text-white w-full py-2 rounded">Login</button>
      </div>
    </div>
  );
}
