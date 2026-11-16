// What each part of the frontend needs from the backend.
// A feature is live once every one of its operations validates against the schema.
export const FEATURES = [
  { key: 'owners', title: 'Find owners', operations: ['findOwners'], needs: 'the owners query' },
  { key: 'ownerPage', title: 'Owner page', operations: ['ownerSummary'], needs: 'the owner query' },
  { key: 'vets', title: 'Vets', operations: ['listVets'], needs: 'the vets query' },
  { key: 'vetPage', title: 'Vet page', operations: ['vetSummary'], needs: 'the vet query' },
  { key: 'ownerPets', title: 'Pets on the owner page', operations: ['ownerPets'], needs: 'the pets of an owner' },
  {
    key: 'ownerDetails',
    title: 'Visits on the owner page',
    operations: ['getOwnerDetails'],
    needs: 'the visits of a pet and the vet of a visit',
  },
  { key: 'vetVisits', title: 'Visits on the vet page', operations: ['vetVisits'], needs: 'the visits of a vet' },
  {
    key: 'vetDetails',
    title: 'Pets and owners on the vet page',
    operations: ['getVetDetails'],
    needs: 'the pet of a visit and the owner of a pet',
  },
  { key: 'ownerAdd', title: 'Add owners', operations: ['addOwner'], needs: 'the addOwner mutation' },
  { key: 'ownerEdit', title: 'Edit owners', operations: ['updateOwner'], needs: 'the updateOwner mutation' },
  { key: 'visitForm', title: 'Book a visit', operations: ['addVisit', 'listVets'], needs: 'the addVisit mutation' },
  {
    key: 'petAdd',
    title: 'Add pets',
    operations: ['addPet', 'listPetTypes'],
    needs: 'the addPet mutation and the petTypes query',
  },
  { key: 'petEdit', title: 'Edit pets', operations: ['updatePet', 'listPetTypes'], needs: 'the updatePet mutation' },
]

export const feature = (key) => FEATURES.find((f) => f.key === key)
