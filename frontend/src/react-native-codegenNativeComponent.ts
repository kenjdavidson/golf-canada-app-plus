export default function codegenNativeComponent(name: string) {
  return {
    displayName: name,
    name,
  }
}
